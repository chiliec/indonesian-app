#!/usr/bin/env ruby
# Set Lancar's age rating to 4+ (all content NONE, all booleans false) via the
# public Connect API (AppInfo → AgeRatingDeclaration, ASC API 1.3+). Idempotent.
require "spaceship"
APP = "6795209576"
Spaceship::ConnectAPI.token = Spaceship::ConnectAPI::Token.create(
  key_id: ENV.fetch("ASC_KEY_ID", "948K3FKL2H"), issuer_id: ENV.fetch("ASC_ISSUER_ID"),
  filepath: File.expand_path(Dir[File.expand_path("../AuthKey_*.p8", __dir__)].first)
)

app = Spaceship::ConnectAPI::App.get(app_id: APP)
info = app.fetch_edit_app_info
puts "Editable AppInfo: #{info.id}"

ard = info.fetch_age_rating_declaration
puts "AgeRatingDeclaration: #{ard.id}"

# 4+ : no objectionable content of any kind, no interactive/exposure features.
attributes = {
  "alcoholTobaccoOrDrugUseOrReferences" => "NONE",
  "contests" => "NONE",
  "gamblingSimulated" => "NONE",
  "gunsOrOtherWeapons" => "NONE",
  "horrorOrFearThemes" => "NONE",
  "matureOrSuggestiveThemes" => "NONE",
  "medicalOrTreatmentInformation" => "NONE",
  "profanityOrCrudeHumor" => "NONE",
  "sexualContentGraphicAndNudity" => "NONE",
  "sexualContentOrNudity" => "NONE",
  "violenceCartoonOrFantasy" => "NONE",
  "violenceRealisticProlongedGraphicOrSadistic" => "NONE",
  "violenceRealistic" => "NONE",
  "advertising" => false,
  "ageAssurance" => false,
  "gambling" => false,
  "healthOrWellnessTopics" => false,
  "lootBox" => false,
  "messagingAndChat" => false,
  "parentalControls" => false,
  "unrestrictedWebAccess" => false,
  "userGeneratedContent" => false,
  "ageRatingOverrideV2" => "NONE",
  "koreaAgeRatingOverride" => "NONE",
  "kidsAgeBand" => nil
}

ard.update(attributes: attributes)
puts "Updated age rating attributes."

info2 = app.fetch_edit_app_info
puts "App Store age rating now: #{info2.app_store_age_rating.inspect}"
