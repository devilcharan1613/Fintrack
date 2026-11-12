import re

with open("app/src/main/assets/index.html", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove Encrypted Backups card in page-datacenter
enc_card_pattern = r'<div class="section-title">Encrypted Backups \(AES-256-GCM\)</div>\s*<div class="card" style="margin-bottom:14px;padding:16px;background:rgba\(6,22,32,0\.85\);border:1\.4px solid rgba\(168,85,247,0\.4\);border-radius:18px;\">.*?</div>\s*</div>'
content = re.sub(enc_card_pattern, "", content, flags=re.DOTALL)

# 2. Update tile subtitle in More
content = content.replace("<small>Encrypted backup, Excel &amp; PDF</small>", "<small>JSON, Excel &amp; PDF</small>")

# 3. Add keyboard-open CSS rule for sheets
keyboard_css = """
.keyboard-open .overlay .sheet {
  max-height: 82vh !important;
  overflow-y: auto !important;
  padding-bottom: 28px !important;
}
"""
if ".keyboard-open .overlay .sheet" not in content:
    content = content.replace("</style>", keyboard_css + "\n</style>", 1)

# 4. Add pencil edit button in txnRowHtml
old_txn_row_end = "<div class=\"txn-date\">'+fmtDateShort(t.date)+'</div>'+\n    '</div>'+\n  '</div>';"
new_txn_row_end = "<div class=\"txn-date\">'+fmtDateShort(t.date)+'</div>'+\n    '</div>'+\n    '<button type=\"button\" class=\"pill-btn\" style=\"padding:4px 8px;font-size:12px;line-height:1;margin-left:8px;background:rgba(0,245,212,0.12);color:#00f5d4;border:1px solid rgba(0,245,212,0.35);border-radius:8px;\" onclick=\"event.stopPropagation();editTxn(\\\''+t.id+'\\\')\" title=\"Edit transaction\">✏️</button>'+\n  '</div>';"
content = content.replace(old_txn_row_end, new_txn_row_end)

# 5. Fix "Travel" button text in TimeMachine
content = content.replace("<button type=\"button\" class=\"btn btn-primary\" onclick=\"applyTimeMachineDate(document.getElementById('timeMachineDateInput').value)\" style=\"padding:10px 16px;height:auto;\">Travel</button>",
                        "<button type=\"button\" class=\"btn btn-primary\" onclick=\"applyTimeMachineDate(document.getElementById('timeMachineDateInput').value)\" style=\"padding:10px 16px;height:auto;\">Inspect Date</button>")

# 6. Add toast in saveReminderSettings
old_reminder_save = "if(window.TracmioBridge&&typeof window.TracmioBridge.scheduleDailyReminder==='function'){\n    var hint=document.getElementById('reminderPermissionHint');\n    if(hint)hint.textContent=config.enabled?'Requesting notification permission and scheduling reminder…':'Turning reminders off…';\n    window.TracmioBridge.scheduleDailyReminder(config.enabled,parts[0],parts[1],config.memeMode);\n  }else{"
new_reminder_save = "if(window.TracmioBridge&&typeof window.TracmioBridge.scheduleDailyReminder==='function'){\n    var hint=document.getElementById('reminderPermissionHint');\n    if(hint)hint.textContent=config.enabled?'Requesting notification permission and scheduling reminder…':'Turning reminders off…';\n    window.TracmioBridge.scheduleDailyReminder(config.enabled,parts[0],parts[1],config.memeMode);\n  }\n  showToast(config.enabled ? '⏰ Daily money check-in scheduled for ' + value + '! 🎉' : '🔕 Reminders turned off', 'success');\n  return;"
content = content.replace(old_reminder_save, new_reminder_save)

# 7. Update End Date formatting in renderCustomTrackersPage
old_end_date = "(tr.endDate ? ' → ' + fmtDate(tr.endDate) : '')"
new_end_date = "(tr.endDate ? ' → <strong style=\"color:#a7f3d0;font-weight:800;\">End: ' + fmtDate(tr.endDate) + '</strong>' : '')"
content = content.replace(old_end_date, new_end_date)

with open("app/src/main/assets/index.html", "w", encoding="utf-8") as f:
    f.write(content)

print("Updates applied successfully to index.html!")
