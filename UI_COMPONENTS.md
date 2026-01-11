# MobileCLI Studio - UI Components Library

**Created:** January 7, 2026
**Purpose:** Define reusable UI components used across all rooms
**Reference:** Godot Engine, Unity Editor, Android Studio

---

## Design System

### Colors

```kotlin
object StudioColors {
    // Backgrounds
    val background = Color(0xFF1A1A2E)      // Main background
    val surface = Color(0xFF16213E)          // Panel backgrounds
    val surfaceDark = Color(0xFF0F0F1A)      // Input fields, deeper surfaces

    // Primary colors
    val primary = Color(0xFFE94560)          // Coral/Red - main accent
    val primaryVariant = Color(0xFFB33B4A)   // Darker primary
    val secondary = Color(0xFF2196F3)        // Blue
    val tertiary = Color(0xFF4CAF50)         // Green - success

    // Status colors
    val success = Color(0xFF4CAF50)          // Green
    val warning = Color(0xFFFF9800)          // Orange
    val error = Color(0xFFF44336)            // Red
    val info = Color(0xFF2196F3)             // Blue

    // Text colors
    val textPrimary = Color(0xFFFFFFFF)      // White
    val textSecondary = Color(0xFF888888)    // Gray
    val textDisabled = Color(0xFF555555)     // Dark gray
    val textHint = Color(0xFF666666)         // Hint text

    // Borders and dividers
    val border = Color(0xFF333333)           // Border color
    val divider = Color(0xFF2A2A2A)          // Divider lines

    // Selection
    val selection = Color(0xFF3A3A5A)        // Selected item
    val hover = Color(0xFF2A2A4A)            // Hover state
}
```

### Dimensions

```kotlin
object StudioDimens {
    // Touch targets (minimum 48dp for accessibility)
    val touchTargetMin = 48.dp
    val buttonHeight = 40.dp
    val iconButtonSize = 44.dp

    // Spacing
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 16.dp
    val spacingLg = 24.dp
    val spacingXl = 32.dp

    // Panel sizes
    val panelMinWidth = 120.dp
    val panelDefaultWidth = 200.dp
    val panelMaxWidth = 400.dp

    // Toolbar
    val toolbarHeight = 48.dp
    val bottomPanelHeight = 150.dp

    // Text sizes
    val textXs = 10.sp
    val textSm = 12.sp
    val textMd = 14.sp
    val textLg = 16.sp
    val textXl = 18.sp
    val textTitle = 20.sp

    // Corner radius
    val cornerSm = 4.dp
    val cornerMd = 8.dp
    val cornerLg = 12.dp

    // Borders
    val borderWidth = 1.dp
    val dividerHeight = 1.dp
}
```

---

## Component 1: StudioToolbar

The top toolbar present in all rooms.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [≡] Room Name    [Tab1] [Tab2] [Tab3]         [Settings] [Claude]│
└──────────────────────────────────────────────────────────────────┘
```

### XML Definition

```xml
<!-- studio_toolbar.xml -->
<LinearLayout
    android:id="@+id/studio_toolbar"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:orientation="horizontal"
    android:background="@color/surface_dark"
    android:gravity="center_vertical"
    android:paddingHorizontal="8dp">

    <!-- Menu Button -->
    <ImageButton
        android:id="@+id/btn_menu"
        android:layout_width="44dp"
        android:layout_height="44dp"
        android:src="@drawable/ic_menu"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="Menu" />

    <!-- Room Title -->
    <TextView
        android:id="@+id/room_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Room Name"
        android:textColor="@color/text_primary"
        android:textSize="16sp"
        android:textStyle="bold"
        android:paddingHorizontal="12dp" />

    <!-- Tab Container (scrollable) -->
    <HorizontalScrollView
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:scrollbars="none">

        <LinearLayout
            android:id="@+id/tab_container"
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:orientation="horizontal"
            android:gravity="center_vertical" />

    </HorizontalScrollView>

    <!-- Settings Button -->
    <ImageButton
        android:id="@+id/btn_settings"
        android:layout_width="44dp"
        android:layout_height="44dp"
        android:src="@drawable/ic_settings"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="Settings" />

    <!-- Claude Button -->
    <Button
        android:id="@+id/btn_claude"
        android:layout_width="wrap_content"
        android:layout_height="36dp"
        android:text="Claude"
        android:textColor="@color/text_primary"
        android:background="@drawable/btn_primary_rounded"
        android:paddingHorizontal="16dp" />

</LinearLayout>
```

### Kotlin Implementation

```kotlin
class StudioToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    interface ToolbarListener {
        fun onMenuClicked()
        fun onSettingsClicked()
        fun onClaudeClicked()
        fun onTabSelected(tabIndex: Int)
    }

    var listener: ToolbarListener? = null
    private var currentTab = 0

    init {
        inflate(context, R.layout.studio_toolbar, this)
        setupClickListeners()
    }

    fun setRoomTitle(title: String) {
        findViewById<TextView>(R.id.room_title).text = title
    }

    fun setTabs(tabs: List<String>) {
        val container = findViewById<LinearLayout>(R.id.tab_container)
        container.removeAllViews()

        tabs.forEachIndexed { index, tabName ->
            val tab = createTabButton(tabName, index)
            container.addView(tab)
        }

        selectTab(0)
    }

    fun selectTab(index: Int) {
        // Update visual state
        currentTab = index
        listener?.onTabSelected(index)
    }

    private fun createTabButton(name: String, index: Int): Button {
        return Button(context).apply {
            text = name
            setTextColor(resources.getColor(R.color.text_secondary, null))
            background = null
            setPadding(24, 8, 24, 8)
            setOnClickListener { selectTab(index) }
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btn_menu).setOnClickListener {
            listener?.onMenuClicked()
        }
        findViewById<View>(R.id.btn_settings).setOnClickListener {
            listener?.onSettingsClicked()
        }
        findViewById<View>(R.id.btn_claude).setOnClickListener {
            listener?.onClaudeClicked()
        }
    }
}
```

---

## Component 2: ResizablePanel

A panel that can be resized by dragging its edge.

### Usage

```
┌────────────┬──────────────────────────────────────┐
│            │                                      │
│   LEFT     │◄─ drag handle                        │
│   PANEL    │                                      │
│            │                                      │
└────────────┴──────────────────────────────────────┘
```

### Kotlin Implementation

```kotlin
class ResizablePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    var resizableEdge: Edge = Edge.RIGHT
    var minSize: Int = 120.dp
    var maxSize: Int = 400.dp
    var currentSize: Int = 200.dp
        private set

    private var dragHandle: View? = null
    private var isDragging = false
    private var startX = 0f
    private var startY = 0f
    private var startSize = 0

    init {
        setupDragHandle()
    }

    private fun setupDragHandle() {
        dragHandle = View(context).apply {
            layoutParams = LayoutParams(
                if (resizableEdge in listOf(Edge.LEFT, Edge.RIGHT)) 8.dp else MATCH_PARENT,
                if (resizableEdge in listOf(Edge.TOP, Edge.BOTTOM)) 8.dp else MATCH_PARENT
            ).apply {
                gravity = when (resizableEdge) {
                    Edge.LEFT -> Gravity.START
                    Edge.RIGHT -> Gravity.END
                    Edge.TOP -> Gravity.TOP
                    Edge.BOTTOM -> Gravity.BOTTOM
                }
            }
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener(::handleDrag)
        }
        addView(dragHandle)
    }

    private fun handleDrag(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                startX = event.rawX
                startY = event.rawY
                startSize = currentSize
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return false

                val delta = when (resizableEdge) {
                    Edge.RIGHT -> (event.rawX - startX).toInt()
                    Edge.LEFT -> (startX - event.rawX).toInt()
                    Edge.BOTTOM -> (event.rawY - startY).toInt()
                    Edge.TOP -> (startY - event.rawY).toInt()
                }

                currentSize = (startSize + delta).coerceIn(minSize, maxSize)
                requestLayout()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (resizableEdge in listOf(Edge.LEFT, Edge.RIGHT)) {
            MeasureSpec.makeMeasureSpec(currentSize, MeasureSpec.EXACTLY)
        } else {
            widthMeasureSpec
        }

        val height = if (resizableEdge in listOf(Edge.TOP, Edge.BOTTOM)) {
            MeasureSpec.makeMeasureSpec(currentSize, MeasureSpec.EXACTLY)
        } else {
            heightMeasureSpec
        }

        super.onMeasure(width, height)
    }
}
```

---

## Component 3: FileTreeView

A hierarchical tree view for files and folders.

### Layout

```
▼ project/
  ▼ app/
    ▼ src/
      ▼ main/
        MainActivity.kt
        Utils.kt
    build.gradle.kts
  settings.gradle.kts
```

### Data Model

```kotlin
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableList<FileNode> = mutableListOf(),
    var isExpanded: Boolean = false,
    var depth: Int = 0
)
```

### Adapter Implementation

```kotlin
class FileTreeAdapter(
    private val onItemClick: (FileNode) -> Unit,
    private val onItemLongClick: (FileNode) -> Boolean
) : RecyclerView.Adapter<FileTreeAdapter.ViewHolder>() {

    private val items = mutableListOf<FileNode>()
    private val expandedPaths = mutableSetOf<String>()

    fun setRoot(root: FileNode) {
        items.clear()
        addNodeRecursive(root, 0)
        notifyDataSetChanged()
    }

    private fun addNodeRecursive(node: FileNode, depth: Int) {
        node.depth = depth
        items.add(node)

        if (node.isDirectory && node.path in expandedPaths) {
            node.isExpanded = true
            node.children.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
                .forEach { addNodeRecursive(it, depth + 1) }
        }
    }

    fun toggleExpand(node: FileNode) {
        if (!node.isDirectory) return

        if (node.path in expandedPaths) {
            expandedPaths.remove(node.path)
        } else {
            expandedPaths.add(node.path)
        }

        // Rebuild tree
        val root = items.firstOrNull() ?: return
        items.clear()
        addNodeRecursive(root, 0)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_tree, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val name: TextView = view.findViewById(R.id.name)
        private val arrow: ImageView = view.findViewById(R.id.arrow)

        fun bind(node: FileNode) {
            // Indentation
            itemView.setPadding(16 + (node.depth * 24), 8, 16, 8)

            // Icon
            icon.setImageResource(
                if (node.isDirectory) R.drawable.ic_folder
                else getFileIcon(node.name)
            )

            // Name
            name.text = node.name

            // Arrow for directories
            arrow.visibility = if (node.isDirectory) View.VISIBLE else View.INVISIBLE
            arrow.rotation = if (node.isExpanded) 90f else 0f

            // Click handlers
            itemView.setOnClickListener {
                if (node.isDirectory) {
                    toggleExpand(node)
                } else {
                    onItemClick(node)
                }
            }
            itemView.setOnLongClickListener { onItemLongClick(node) }
        }

        private fun getFileIcon(name: String): Int {
            return when {
                name.endsWith(".kt") -> R.drawable.ic_kotlin
                name.endsWith(".java") -> R.drawable.ic_java
                name.endsWith(".xml") -> R.drawable.ic_xml
                name.endsWith(".json") -> R.drawable.ic_json
                name.endsWith(".gradle.kts") -> R.drawable.ic_gradle
                name.endsWith(".md") -> R.drawable.ic_markdown
                else -> R.drawable.ic_file
            }
        }
    }
}
```

### XML Layout for Item

```xml
<!-- item_file_tree.xml -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:background="?attr/selectableItemBackground"
    android:minHeight="40dp">

    <ImageView
        android:id="@+id/arrow"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:src="@drawable/ic_arrow_right"
        android:tint="@color/text_secondary" />

    <ImageView
        android:id="@+id/icon"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_marginStart="4dp" />

    <TextView
        android:id="@+id/name"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="8dp"
        android:textColor="@color/text_primary"
        android:textSize="13sp"
        android:singleLine="true"
        android:ellipsize="middle" />

</LinearLayout>
```

---

## Component 4: PropertyEditor

Displays and edits object properties (like Unity Inspector).

### Layout

```
┌──────────────────────────────────────┐
│ PROPERTIES                           │
├──────────────────────────────────────┤
│ Position                             │
│ X: [0.0    ] Y: [0.0    ] Z: [0.0  ] │
│                                      │
│ Rotation                             │
│ X: [0.0    ] Y: [0.0    ] Z: [0.0  ] │
│                                      │
│ Scale                                │
│ X: [1.0    ] Y: [1.0    ] Z: [1.0  ] │
│                                      │
│ Color                                │
│ [■] #FFFFFF                          │
│                                      │
│ Visible  [✓]                         │
└──────────────────────────────────────┘
```

### Data Model

```kotlin
sealed class PropertyValue {
    data class Text(val value: String) : PropertyValue()
    data class Number(val value: Float) : PropertyValue()
    data class Boolean(val value: kotlin.Boolean) : PropertyValue()
    data class Color(val value: Int) : PropertyValue()
    data class Vector3(val x: Float, val y: Float, val z: Float) : PropertyValue()
    data class Enum(val value: String, val options: List<String>) : PropertyValue()
}

data class Property(
    val name: String,
    val key: String,
    val value: PropertyValue,
    val category: String = ""
)
```

### Adapter Implementation

```kotlin
class PropertyAdapter(
    private val onPropertyChanged: (String, PropertyValue) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_TEXT = 1
        const val TYPE_NUMBER = 2
        const val TYPE_BOOLEAN = 3
        const val TYPE_COLOR = 4
        const val TYPE_VECTOR3 = 5
        const val TYPE_ENUM = 6
    }

    private val items = mutableListOf<Any>() // String (category) or Property

    fun setProperties(properties: List<Property>) {
        items.clear()

        // Group by category
        val grouped = properties.groupBy { it.category }
        grouped.forEach { (category, props) ->
            if (category.isNotEmpty()) {
                items.add(category)
            }
            items.addAll(props)
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when {
            item is String -> TYPE_CATEGORY
            item is Property -> when (item.value) {
                is PropertyValue.Text -> TYPE_TEXT
                is PropertyValue.Number -> TYPE_NUMBER
                is PropertyValue.Boolean -> TYPE_BOOLEAN
                is PropertyValue.Color -> TYPE_COLOR
                is PropertyValue.Vector3 -> TYPE_VECTOR3
                is PropertyValue.Enum -> TYPE_ENUM
            }
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY -> CategoryViewHolder(
                inflater.inflate(R.layout.item_property_category, parent, false)
            )
            TYPE_VECTOR3 -> Vector3ViewHolder(
                inflater.inflate(R.layout.item_property_vector3, parent, false)
            )
            TYPE_BOOLEAN -> BooleanViewHolder(
                inflater.inflate(R.layout.item_property_boolean, parent, false)
            )
            TYPE_COLOR -> ColorViewHolder(
                inflater.inflate(R.layout.item_property_color, parent, false)
            )
            else -> TextViewHolder(
                inflater.inflate(R.layout.item_property_text, parent, false)
            )
        }
    }

    // ... ViewHolder implementations for each type
}
```

---

## Component 5: ConsolePanel

Output console with tabs for different outputs.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [Console] [Build] [Logcat] [Problems]                      [Clear]│
├──────────────────────────────────────────────────────────────────┤
│ > Build started at 14:32:05                                      │
│ > Compiling resources...                                         │
│ > Compiling Kotlin sources...                                    │
│ [E] Error: Cannot find symbol 'foo'                              │
│ > BUILD FAILED in 12s                                            │
└──────────────────────────────────────────────────────────────────┘
```

### Data Model

```kotlin
enum class LogLevel { INFO, WARNING, ERROR, DEBUG }

data class LogEntry(
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Console"
)
```

### Implementation

```kotlin
class ConsolePanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val tabs = mutableListOf<String>()
    private val logs = mutableMapOf<String, MutableList<LogEntry>>()
    private var currentTab = "Console"

    private lateinit var tabContainer: LinearLayout
    private lateinit var outputView: RecyclerView
    private lateinit var adapter: LogAdapter

    init {
        orientation = VERTICAL
        inflate(context, R.layout.console_panel, this)
        setupViews()
    }

    private fun setupViews() {
        tabContainer = findViewById(R.id.tab_container)
        outputView = findViewById(R.id.output_view)

        adapter = LogAdapter()
        outputView.adapter = adapter
        outputView.layoutManager = LinearLayoutManager(context)
    }

    fun setTabs(tabNames: List<String>) {
        tabs.clear()
        tabs.addAll(tabNames)
        tabNames.forEach { logs[it] = mutableListOf() }
        rebuildTabs()
    }

    fun log(message: String, level: LogLevel = LogLevel.INFO, tab: String = "Console") {
        val entry = LogEntry(message, level, source = tab)
        logs.getOrPut(tab) { mutableListOf() }.add(entry)

        if (tab == currentTab) {
            adapter.addEntry(entry)
            outputView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    fun clear(tab: String = currentTab) {
        logs[tab]?.clear()
        if (tab == currentTab) {
            adapter.clear()
        }
    }

    private fun selectTab(tab: String) {
        currentTab = tab
        adapter.setEntries(logs[tab] ?: emptyList())
        rebuildTabs()
    }

    private fun rebuildTabs() {
        tabContainer.removeAllViews()
        tabs.forEach { tabName ->
            val button = Button(context).apply {
                text = tabName
                setTextColor(
                    if (tabName == currentTab)
                        resources.getColor(R.color.text_primary, null)
                    else
                        resources.getColor(R.color.text_secondary, null)
                )
                background = null
                setOnClickListener { selectTab(tabName) }
            }
            tabContainer.addView(button)
        }
    }
}
```

---

## Component 6: CodeEditor (WebView-based)

Monaco Editor or CodeMirror in a WebView for syntax highlighting.

### HTML Template

```html
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs/editor/editor.main.css">
    <style>
        body {
            margin: 0;
            padding: 0;
            background: #0f0f1a;
        }
        #editor {
            width: 100%;
            height: 100vh;
        }
    </style>
</head>
<body>
    <div id="editor"></div>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs/loader.js"></script>
    <script>
        require.config({ paths: { vs: 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.44.0/min/vs' } });

        require(['vs/editor/editor.main'], function () {
            // Define dark theme
            monaco.editor.defineTheme('studio-dark', {
                base: 'vs-dark',
                inherit: true,
                rules: [],
                colors: {
                    'editor.background': '#0f0f1a',
                    'editor.lineHighlightBackground': '#1a1a2e',
                }
            });

            window.editor = monaco.editor.create(document.getElementById('editor'), {
                value: '',
                language: 'kotlin',
                theme: 'studio-dark',
                fontSize: 14,
                minimap: { enabled: false },
                automaticLayout: true,
                scrollBeyondLastLine: false,
                wordWrap: 'on'
            });

            // Report changes to Android
            window.editor.onDidChangeModelContent(function() {
                if (window.Android) {
                    window.Android.onContentChanged(window.editor.getValue());
                }
            });
        });

        // Functions callable from Android
        function setContent(content) {
            if (window.editor) {
                window.editor.setValue(content);
            }
        }

        function setLanguage(lang) {
            if (window.editor) {
                monaco.editor.setModelLanguage(window.editor.getModel(), lang);
            }
        }

        function getContent() {
            return window.editor ? window.editor.getValue() : '';
        }
    </script>
</body>
</html>
```

### Kotlin WebView Wrapper

```kotlin
class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs) {

    interface EditorListener {
        fun onContentChanged(content: String)
        fun onSaveRequested()
    }

    var listener: EditorListener? = null
    private var isLoaded = false
    private var pendingContent: String? = null

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        addJavascriptInterface(object {
            @JavascriptInterface
            fun onContentChanged(content: String) {
                post { listener?.onContentChanged(content) }
            }
        }, "Android")

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                isLoaded = true
                pendingContent?.let { setContent(it) }
                pendingContent = null
            }
        }

        loadUrl("file:///android_asset/editor.html")
    }

    fun setContent(content: String) {
        if (isLoaded) {
            val escaped = content.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
            evaluateJavascript("setContent(\"$escaped\")", null)
        } else {
            pendingContent = content
        }
    }

    fun setLanguage(language: String) {
        if (isLoaded) {
            evaluateJavascript("setLanguage('$language')", null)
        }
    }

    fun getContent(callback: (String) -> Unit) {
        evaluateJavascript("getContent()") { result ->
            val content = result?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?: ""
            callback(content)
        }
    }
}
```

---

## Component 7: TabBar

Horizontal scrollable tabs for files/views.

### Layout

```
┌──────────────────────────────────────────────────────────────────┐
│ [MainActivity.kt] [× ] [Utils.kt] [× ] [activity_main.xml] [× ] │
└──────────────────────────────────────────────────────────────────┘
```

### Implementation

```kotlin
class TabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : HorizontalScrollView(context, attrs) {

    data class Tab(
        val id: String,
        val title: String,
        var isDirty: Boolean = false
    )

    interface TabListener {
        fun onTabSelected(tab: Tab)
        fun onTabClosed(tab: Tab)
    }

    var listener: TabListener? = null
    private val tabs = mutableListOf<Tab>()
    private var selectedTabId: String? = null
    private val container: LinearLayout

    init {
        isHorizontalScrollBarEnabled = false
        container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 4, 4, 4)
        }
        addView(container)
    }

    fun addTab(tab: Tab) {
        tabs.add(tab)
        val tabView = createTabView(tab)
        container.addView(tabView)
        selectTab(tab.id)
    }

    fun selectTab(id: String) {
        selectedTabId = id
        rebuildTabs()
        tabs.find { it.id == id }?.let { listener?.onTabSelected(it) }
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index == -1) return

        val tab = tabs.removeAt(index)
        listener?.onTabClosed(tab)

        if (id == selectedTabId && tabs.isNotEmpty()) {
            selectTab(tabs.getOrNull(index) ?: tabs.last()).id)
        }

        rebuildTabs()
    }

    fun setTabDirty(id: String, dirty: Boolean) {
        tabs.find { it.id == id }?.isDirty = dirty
        rebuildTabs()
    }

    private fun createTabView(tab: Tab): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.item_tab, container, false).apply {
                findViewById<TextView>(R.id.tab_title).text =
                    if (tab.isDirty) "● ${tab.title}" else tab.title

                val isSelected = tab.id == selectedTabId
                setBackgroundColor(
                    if (isSelected)
                        resources.getColor(R.color.surface, null)
                    else
                        Color.TRANSPARENT
                )

                setOnClickListener { selectTab(tab.id) }
                findViewById<View>(R.id.btn_close).setOnClickListener {
                    closeTab(tab.id)
                }
            }
    }

    private fun rebuildTabs() {
        container.removeAllViews()
        tabs.forEach { container.addView(createTabView(it)) }
    }
}
```

---

## Usage in Room Activities

Each room extends BaseRoomActivity and uses these components:

```kotlin
abstract class BaseRoomActivity : AppCompatActivity() {

    protected lateinit var toolbar: StudioToolbar
    protected lateinit var leftPanel: ResizablePanel
    protected lateinit var rightPanel: ResizablePanel
    protected lateinit var bottomPanel: ConsolePanel
    protected lateinit var claudePanel: SlidingPaneLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.room_base)

        setupToolbar()
        setupPanels()
        setupClaude()
        setupRoomContent()
    }

    abstract fun getRoomTitle(): String
    abstract fun getRoomTabs(): List<String>
    abstract fun setupRoomContent()

    protected fun log(message: String, level: LogLevel = LogLevel.INFO) {
        bottomPanel.log(message, level)
    }

    protected fun openClaude() {
        claudePanel.openPane()
    }

    protected fun closeClaude() {
        claudePanel.closePane()
    }
}
```

---

## File Icons

Icons needed for file tree:

| Icon | File Types |
|------|------------|
| ic_folder | Directories |
| ic_kotlin | .kt, .kts |
| ic_java | .java |
| ic_xml | .xml |
| ic_json | .json |
| ic_javascript | .js, .jsx |
| ic_typescript | .ts, .tsx |
| ic_html | .html, .htm |
| ic_css | .css, .scss |
| ic_python | .py |
| ic_markdown | .md |
| ic_gradle | .gradle, .gradle.kts |
| ic_file | Default |

---

**Document Status:** Complete
**Last Updated:** January 7, 2026
