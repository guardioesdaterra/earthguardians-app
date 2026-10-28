import SwiftUI
import EventKitUI
import EGCore

/// Retroactive conformance so URLs can drive `.sheet(item:)` (iOS 16).
extension URL: Identifiable {
    public var id: String { absoluteString }
}

/**
 * Event detail: hero image, real date, description and native iOS actions —
 * system calendar sheet (EventKit), share sheet, open the event page in-app.
 */
struct EventDetailScreen: View {
    @EnvironmentObject private var graph: AppGraph
    @EnvironmentObject private var router: Router
    @Environment(\.egPalette) private var palette
    @Environment(\.dismiss) private var dismiss

    let eventURL: String

    @State private var showCalendarSheet = false
    @State private var shareItem: URL?

    private var event: EgEvent? {
        if case .ready(let feed, _) = graph.content.feed {
            return feed.events.first { $0.url == eventURL }
        }
        return nil
    }

    var body: some View {
        Group {
            if let event {
                detail(event)
            } else {
                VStack {
                    EGBackButton { dismiss() }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                    EmptyState(
                        systemImage: "arrow.up.forward.square",
                        title: "Event unavailable",
                        message: "This event fell out of the latest feed sync. Try refreshing from the Events tab."
                    )
                    Spacer()
                }
            }
        }
        .background(palette.background.ignoresSafeArea())
        .sheet(item: $shareItem) { url in
            ActivityShareSheet(items: [url])
        }
        .sheet(isPresented: $showCalendarSheet) {
            EventEditView(event: event)
        }
    }

    // MARK: Detail body

    @ViewBuilder
    private func detail(_ event: EgEvent) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    EGBackButton { dismiss() }
                    Spacer()
                    Button {
                        UIC.feedback()
                        shareItem = event.url.asURL
                    } label: {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(palette.onBackground)
                    }
                    .buttonStyle(PressableStyle())
                    .accessibilityLabel("Share")
                }
                .padding(.horizontal, 12)

                EgImage(url: event.imageUrl, cornerRadius: 26)
                    .frame(height: 250)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        EgPill(egShortDate(event.startsAt), tone: .green)
                        if let location = event.location, !location.isEmpty {
                            EgPill(location, tone: .neutral)
                        }
                    }

                    Text(event.title)
                        .font(.system(size: 28, weight: .black))
                        .kerning(-0.4)
                        .foregroundColor(palette.onBackground)
                        .fixedSize(horizontal: false, vertical: true)

                    if let description = event.description, !description.isEmpty {
                        Text(description)
                            .font(.system(size: 16))
                            .lineSpacing(6)
                            .foregroundColor(palette.muted)
                    }

                    HStack(spacing: 10) {
                        EgPrimaryButton("Save to calendar", tone: .green, systemImage: "calendar.badge.plus") {
                            UIC.feedback()
                            showCalendarSheet = true
                        }
                        EgPrimaryButton("Open page", tone: .red, systemImage: "arrow.up.forward") {
                            router.push(.section(event.url))
                        }
                    }
                    .padding(.top, 4)

                    GlassCard {
                        Text("Crew events are hosted by Earth Guardians regional councils. Reach out on the event page for Zoom links, sign ups and volunteer slots.")
                            .font(.system(size: 13))
                            .lineSpacing(4)
                            .foregroundColor(palette.muted)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 6)
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - EventKit bridge

private struct EventEditView: UIViewControllerRepresentable {
    @Environment(\.dismiss) private var dismiss
    let event: EgEvent?

    func makeUIViewController(context: Context) -> EKEventEditViewController {
        let controller = EKEventEditViewController()
        let ekEvent = EKEvent(eventStore: context.coordinator.store)
        ekEvent.title = event?.title
        if let startsAt = event?.startsAt {
            ekEvent.startDate = startsAt
            ekEvent.endDate = startsAt.addingTimeInterval(2 * 3600)
        }
        if let location = event?.location { ekEvent.location = location }
        ekEvent.notes = event?.url
        controller.event = ekEvent
        controller.eventStore = context.coordinator.store
        controller.editViewDelegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ uiViewController: EKEventEditViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    final class Coordinator: NSObject, EKEventEditViewDelegate {
        let store = EKEventStore()

        func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
            // EKEventEditViewController requests its own calendar access prompt.
            controller.dismiss(animated: true)
        }
    }
}

// MARK: - Share sheet bridge

struct ActivityShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

extension String {
    var asURL: URL? { URL(string: self) }
}
