#!/usr/bin/env ruby
# Emits SQL to seed a realistic Lancar state for store screenshots:
# mastery spread across modules, Leitner boxes 1-6 populated, and some cards
# due today so Beranda shows the review banner. Usage:
#   ruby gen_seed_sql.rb <today_epoch_day> > seed.sql
today = Integer(ARGV[0] || (Time.now.to_i / 86400))
now_ms = Time.now.to_i * 1000
COUNTS = { 1 => 256, 2 => 249, 3 => 269, 4 => 203, 5 => 236, 6 => 202, 7 => 139, 8 => 242 }

puts "DELETE FROM card_progress;"
puts "INSERT OR REPLACE INTO app_settings(key,value) VALUES ('onboarding_seen','true');"
puts "INSERT OR REPLACE INTO app_settings(key,value) VALUES ('display_name','Sari');"
puts "INSERT OR REPLACE INTO app_settings(key,value) VALUES ('accent','TERRACOTTA');"
puts "BEGIN;"

COUNTS.each do |m, count|
  mastered = (count * 0.55).to_i
  (1..mastered).each do |i|
    id = format("module-%d-%04d", m, i)
    correct = 1 + (i % 3)          # 1..3 correct
    wrong   = i % 2                # 0/1
    seen    = correct + wrong + (i % 2)
    box     = (i % 6) + 1          # 1..6
    # ~4 per module land due today (across low boxes) -> review banner
    due = if i <= 4
            today
          else
            today + box * 2        # future
          end
    puts "INSERT OR REPLACE INTO card_progress(card_id,seen,correct,wrong,last_seen,box,due_day) " \
         "VALUES ('#{id}',#{seen},#{correct},#{wrong},#{now_ms},#{box},#{due});"
  end
end
puts "COMMIT;"
