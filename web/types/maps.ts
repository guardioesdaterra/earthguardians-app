export interface CrewRegion {
  id: string
  region: string
  latitude: number
  longitude: number
  activeCrews: number
  inactiveCrews: number
  totalMembers: number
  countries: number
}

export interface CrewLocation {
  name: string
  country: string
  city: string
  state?: string | null
  region: string
  status: string
  lat: number
  lng: number
}

export interface ProjectGrant {
  project_title: string
  country_province: string
  latitude: number
  longitude: number
  direct_beneficiaries: number
  indirect_beneficiaries: number
}

export interface MapsDataset {
  schemaVersion: number
  regions: CrewRegion[]
  crews: CrewLocation[]
  grants: ProjectGrant[]
}

export type MapSelection =
  | { type: 'region'; value: CrewRegion }
  | { type: 'crew'; value: CrewLocation }
  | { type: 'grant'; value: ProjectGrant }
