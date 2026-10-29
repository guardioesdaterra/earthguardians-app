# Fotos da homepage oficial — fontes e assets

O proprietário autorizou explicitamente o uso das imagens de earthguardians.org nesta tarefa. As seis fotos abaixo foram inspecionadas na homepage oficial e são empacotadas localmente para uso offline. As versões incluídas são JPEG otimizadas, sem texto ou composição adicionados; o app aplica o recorte responsivo e a opacidade no momento da renderização.

| Foto | URL de origem na homepage | Android | iOS |
|---|---|---|---|
| Grupo comunitário ao ar livre | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/36762f38-8212-43bd-84b2-5bb8a6fc1ff4/IMG_9883+%281%29.JPG?format=800w | `app/src/main/res/drawable-nodpi/eg_home_community.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeCommunity.imageset/eg-home-community.jpg` |
| Ação de jovens em Benin / So-Ava | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/d45ea297-2712-410c-8710-f8069e1eeb69/Earth+Guardians+Benin_Section+So-Ava+17.jpeg?format=800w | `app/src/main/res/drawable-nodpi/eg_home_benin_action.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeBeninAction.imageset/eg-home-benin-action.jpg` |
| Folhas e luz natural | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/1709268107661-9EPQ9RVOW09Y3AF9H61G/image-asset.jpeg?format=800w | `app/src/main/res/drawable-nodpi/eg_home_leaves.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeLeaves.imageset/eg-home-leaves.jpg` |
| Encontro comunitário visto de cima (DJI_0641) | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/6ade35a1-ec2b-4c55-9c0e-5d5ff9826d6a/DJI_0641.JPG?format=800w | `app/src/main/res/drawable-nodpi/eg_home_fieldwork.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeFieldwork.imageset/eg-home-fieldwork.jpg` |
| Jovens reunidos (IMG_6713) | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/865820ae-8a77-49c5-b975-7713cd6d261f/IMG_6713.jpeg?format=800w | `app/src/main/res/drawable-nodpi/eg_home_youth_gathering.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeYouthGathering.imageset/eg-home-youth-gathering.jpg` |
| Marcha por justiça climática | https://images.squarespace-cdn.com/content/v1/5e66bef295469c229eeb5cfc/311b57c0-4b23-47a0-86c3-5a509bb19005/pexels-vincent-ma-janssen-2561628+%281%29.jpg?format=800w | `app/src/main/res/drawable-nodpi/eg_home_climate_action.jpg` | `ios/EarthGuardians/Assets.xcassets/EGHomeClimateAction.imageset/eg-home-climate-action.jpg` |

`OfficialSitePhotoCollage` reutiliza as seis fotos em posições alternadas com opacidade de aproximadamente 7,5% no fundo geral e 13–15% nos painéis ilustrados. O texto e os controles ficam em camadas opacas à frente; há também uma gradação escura sobre o painel para preservar legibilidade.
