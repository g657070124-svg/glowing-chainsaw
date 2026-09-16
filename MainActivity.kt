package com.example.qeyasat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil

data class MaterialItem(var name: String, var measurement: String, var count: String)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("qiyasat_data", Context.MODE_PRIVATE) }
    private val items = mutableListOf<MaterialItem>()
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadItems()
        showHome()
    }

    private fun baseLayout(title: String): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(TextView(this).apply {
            text = title
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 24)
        })
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        ScrollView(this).also { scroll ->
            scroll.addView(content)
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        return root
    }

    private fun button(text: String, onClick: () -> Unit) =
        Button(this).apply { this.text = text; textSize = 17f; setOnClickListener { onClick() } }

    private fun showHome() {
        val root = baseLayout("📏 قياسات - ادارة المواد")
        root.addView(button("📋 إدارة المواد والطلبات") { showOrders() })
        root.addView(button("🪶 حساب عدد الريش") { showReeshCalculator() })
        root.addView(button("⚖️ حساب وزن الحديد") { showWeightCalculator() })
        root.addView(button("ℹ️ عن البرنامج") {
            Toast.makeText(this, "قياسات\nالمطور: محمد الفاتح", Toast.LENGTH_LONG).show()
        })
        setContentView(root)
    }

    private fun showOrders() {
        val root = baseLayout("📋 إدارة المواد والطلبات")
        root.addView(button("➕ إضافة مادة") { showItemDialog(null, -1) })
        root.addView(button("📤 مشاركة القائمة") { shareItems() })
        root.addView(button("🗑️ حذف كل المواد") {
            items.clear(); saveItems(); showOrders()
        })
        root.addView(button("⬅️ رجوع") { showHome() })

        if (items.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "لا توجد مواد مضافة بعد."; textSize = 18f; gravity = Gravity.CENTER
                setPadding(8, 30, 8, 30)
            })
        } else {
            items.forEachIndexed { index, item ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(12, 12, 12, 12)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0xFFF1F1F1.toInt()); cornerRadius = 18f
                    }
                }
                row.addView(TextView(this).apply {
                    text = "${index + 1}- ${item.name} | القياس: ${if (item.measurement.isBlank()) "—" else item.measurement} | العدد: ${item.count}"
                    textSize = 18f
                    setTextIsSelectable(true)
                })
                val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
                actions.addView(button("✏️ تعديل") { showItemDialog(item, index) })
                actions.addView(button("🗑️ حذف") {
                    items.removeAt(index); saveItems(); showOrders()
                })
                row.addView(actions)
                content.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 14) })
            }
        }
        setContentView(root)
    }

    private fun showItemDialog(existing: MaterialItem?, index: Int) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 10, 30, 10) }
        val name = EditText(this).apply { hint = "اسم المادة"; setText(existing?.name ?: ""); textSize = 17f }
        val measurement = EditText(this).apply { hint = "القياس (مثال: 350 أو طول 4 أمتار)"; setText(existing?.measurement ?: ""); textSize = 17f }
        val count = EditText(this).apply { hint = "العدد (مثال: 6 قطعة أو 5 كراتين)"; setText(existing?.count ?: ""); textSize = 17f }
        box.addView(name); box.addView(measurement); box.addView(count)

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(if (existing == null) "إضافة مادة" else "تعديل المادة")
            .setView(box).setNegativeButton("إلغاء", null).setPositiveButton("حفظ", null).create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                val m = measurement.text.toString().trim()
                val c = count.text.toString().trim()
                if (n.isEmpty() || c.isEmpty()) {
                    Toast.makeText(this, "اكتب اسم المادة والعدد", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (existing == null) items.add(MaterialItem(n, m, c))
                else { existing.name = n; existing.measurement = m; existing.count = c }
                saveItems(); dialog.dismiss(); showOrders()
            }
        }
        dialog.show()
    }

    private fun showReeshCalculator() {
        val root = baseLayout("🪶 حساب عدد الريش")
        root.addView(TextView(this).apply {
            text = "ارتفاع الريشة ثابت: 10.5 سم\nالعدد = تقريب للأعلى (الارتفاع ÷ 10.5)"
            textSize = 18f; setPadding(8, 8, 8, 20)
        })
        val height = EditText(this).apply {
            hint = "أدخل الارتفاع بالسنتيمتر"
            inputType = 2 or 8192; textSize = 18f
        }
        root.addView(height)
        val result = TextView(this).apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(8,20,8,20) }
        root.addView(button("احسب") {
            val h = height.text.toString().toDoubleOrNull()
            result.text = if (h == null || h <= 0) "أدخل ارتفاعاً صحيحاً" else "عدد الريش = ${ceil(h / 10.5).toInt()}"
        })
        root.addView(result); root.addView(button("⬅️ رجوع") { showHome() })
        setContentView(root)
    }

    private fun showWeightCalculator() {
        val root = baseLayout("⚖️ حساب وزن الحديد")
        root.addView(TextView(this).apply {
            text = "الحساب عملي بالغرام/م²:\n4 ديسم = 4800 غ/م²\n4.5 ديسم = 5300 غ/م²"
            textSize = 18f; setPadding(8,8,8,20)
        })
        val width = EditText(this).apply { hint = "العرض بالمتر"; inputType = 2 or 8192; textSize = 18f }
        val height = EditText(this).apply { hint = "الارتفاع بالمتر"; inputType = 2 or 8192; textSize = 18f }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("4 ديسم — 4800 غ/م²", "4.5 ديسم — 5300 غ/م²"))
        root.addView(width); root.addView(height); root.addView(spinner)
        val result = TextView(this).apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(8,20,8,20) }
        root.addView(button("احسب الوزن") {
            val w = width.text.toString().toDoubleOrNull()
            val h = height.text.toString().toDoubleOrNull()
            if (w == null || h == null || w <= 0 || h <= 0) result.text = "أدخل العرض والارتفاع بشكل صحيح"
            else {
                val g = if (spinner.selectedItemPosition == 0) 4800.0 else 5300.0
                val kg = w * h * g / 1000.0
                result.text = "المساحة = ${w * h} م²\nالوزن = ${"%.3f".format(kg)} كغ"
            }
        })
        root.addView(result); root.addView(button("⬅️ رجوع") { showHome() })
        setContentView(root)
    }

    private fun shareItems() {
        if (items.isEmpty()) { Toast.makeText(this, "لا توجد مواد لمشاركتها", Toast.LENGTH_SHORT).show(); return }
        val text = buildString {
            append("📏 قياسات - ادارة المواد\n\n")
            items.forEachIndexed { i, x ->
                append("${i+1}- ${x.name} | القياس: ${if (x.measurement.isBlank()) "—" else x.measurement} | العدد: ${x.count}\n")
            }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }, "مشاركة القائمة"))
    }

    private fun saveItems() {
        val a = JSONArray()
        items.forEach { x -> a.put(JSONObject().apply { put("name",x.name); put("measurement",x.measurement); put("count",x.count) }) }
        prefs.edit().putString("items", a.toString()).apply()
    }

    private fun loadItems() {
        val raw = prefs.getString("items", null) ?: return
        try {
            val a = JSONArray(raw)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                items.add(MaterialItem(o.optString("name"), o.optString("measurement"), o.optString("count")))
            }
        } catch (_: Exception) { items.clear() }
    }

    @Deprecated("Deprecated in Android API 33")
    override fun onBackPressed() { showHome() }
}
