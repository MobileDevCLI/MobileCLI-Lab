# MobileCLI Studio - Room Specifications

**Created:** January 7, 2026
**Purpose:** Document every button, feature, and function in each room
**Status:** Complete Specification

---

## Room 1: Mobile Apps (APK Builder)

**Priority:** #1 (Core feature of MobileCLI)
**Target Users:** Android developers
**Core Value:** Build APKs directly on phone

### Complete UI Element List

#### Top Toolbar

| ID | Element | Type | Label | Function | Implementation |
|----|---------|------|-------|----------|----------------|
| T1 | btn_menu | ImageButton | ≡ | Open navigation drawer | DrawerLayout.openDrawer(GravityCompat.START) |
| T2 | room_title | TextView | "Mobile Apps" | Display room name | Static text |
| T3 | btn_new_project | Button | + New | Create new Android project | Show NewProjectDialog |
| T4 | btn_open_project | Button | Open | Open existing project | Show ProjectPickerDialog |
| T5 | btn_save | ImageButton | 💾 | Save current file | FileManager.save(currentFile) |
| T6 | btn_save_all | ImageButton | 💾+ | Save all open files | FileManager.saveAll() |
| T7 | btn_undo | ImageButton | ↶ | Undo last edit | editor.undo() |
| T8 | btn_redo | ImageButton | ↷ | Redo last edit | editor.redo() |
| T9 | btn_build | Button | ▶ Build | Build debug APK | runGradle("assembleDebug") |
| T10 | btn_build_release | Button | 📦 Release | Build release APK | runGradle("assembleRelease") |
| T11 | btn_run | Button | ▶ Run | Build + Install + Launch | buildAndRun() |
| T12 | btn_stop | ImageButton | ⏹ | Stop running build/app | cancelBuild() |
| T13 | btn_settings | ImageButton | ⚙ | Open room settings | startActivity(SettingsActivity) |
| T14 | btn_claude | Button | 🤖 Claude | Toggle Claude panel | claudePanel.toggle() |

#### Left Panel: Project Files

| ID | Element | Type | Function | Implementation |
|----|---------|------|----------|----------------|
| L1 | file_tree | RecyclerView | Display project file tree | FileTreeAdapter |
| L2 | btn_refresh | ImageButton | Refresh file tree | fileTree.reload() |
| L3 | btn_collapse_all | ImageButton | Collapse all folders | fileTree.collapseAll() |
| L4 | btn_new_file | ImageButton | Create new file | showNewFileDialog() |
| L5 | btn_new_folder | ImageButton | Create new folder | showNewFolderDialog() |

##### File Tree Context Menu (Long Press)

| ID | Action | Function | Implementation |
|----|--------|----------|----------------|
| LM1 | Open | Open file in editor | openFile(file) |
| LM2 | Open With... | Choose app to open | showOpenWithDialog(file) |
| LM3 | Rename | Rename file/folder | showRenameDialog(file) |
| LM4 | Delete | Delete file/folder | showDeleteConfirmDialog(file) |
| LM5 | Copy | Copy file path | clipboard.setText(file.path) |
| LM6 | Copy Path | Copy full path | clipboard.setText(file.absolutePath) |
| LM7 | Move To... | Move to different location | showMoveDialog(file) |
| LM8 | New File Here | Create file in this folder | showNewFileDialog(file.parent) |
| LM9 | New Folder Here | Create folder here | showNewFolderDialog(file.parent) |
| LM10 | Find in Files | Search within folder | showFindInFilesDialog(file) |

#### Center Panel: Editor Area

| ID | Element | Type | Function | Implementation |
|----|---------|------|----------|----------------|
| C1 | tab_bar | TabBar | Show open files | TabBar component |
| C2 | code_editor | WebView | Monaco editor | CodeEditorView |
| C3 | editor_tabs | TabLayout | Code/Design/Split | ViewPager2 |
| C4 | design_preview | WebView | Layout preview | LayoutPreview |
| C5 | split_handle | View | Resize split view | Drag to resize |

##### Tab Bar Actions

| ID | Action | Trigger | Function | Implementation |
|----|--------|---------|----------|----------------|
| CB1 | Select Tab | Click | Switch to file | tabBar.selectTab(id) |
| CB2 | Close Tab | Click X | Close file | tabBar.closeTab(id) |
| CB3 | Close Others | Long press > menu | Close other tabs | tabBar.closeOthers(id) |
| CB4 | Close All | Long press > menu | Close all tabs | tabBar.closeAll() |

##### Editor Actions

| ID | Action | Trigger | Function | Implementation |
|----|--------|---------|----------|----------------|
| CE1 | Find | Ctrl+F | Show find bar | editor.showFind() |
| CE2 | Replace | Ctrl+H | Show replace bar | editor.showReplace() |
| CE3 | Go to Line | Ctrl+G | Jump to line | editor.goToLine() |
| CE4 | Format | Ctrl+Shift+F | Format code | editor.format() |
| CE5 | Comment | Ctrl+/ | Toggle comment | editor.toggleComment() |
| CE6 | Duplicate | Ctrl+D | Duplicate line | editor.duplicateLine() |
| CE7 | Delete Line | Ctrl+Shift+K | Delete line | editor.deleteLine() |
| CE8 | Move Up | Alt+Up | Move line up | editor.moveLineUp() |
| CE9 | Move Down | Alt+Down | Move line down | editor.moveLineDown() |

#### Right Panel: Component Tree & Properties

| ID | Element | Type | Function | Implementation |
|----|---------|------|----------|----------------|
| R1 | component_tree | RecyclerView | XML element hierarchy | ComponentTreeAdapter |
| R2 | properties_panel | RecyclerView | Selected element properties | PropertyAdapter |
| R3 | panel_tabs | TabLayout | Tree / Properties toggle | TabLayout |

##### Component Tree Actions

| ID | Action | Trigger | Function | Implementation |
|----|--------|---------|----------|----------------|
| RT1 | Select | Click | Select element | highlightInPreview() |
| RT2 | Expand/Collapse | Click arrow | Toggle children | toggleExpand() |
| RT3 | Rename | Long press > Rename | Change ID | showRenameDialog() |
| RT4 | Delete | Long press > Delete | Remove element | deleteElement() |
| RT5 | Wrap With | Long press > Wrap | Wrap in container | showWrapDialog() |
| RT6 | Move Up | Long press > Move Up | Move in hierarchy | moveUp() |
| RT7 | Move Down | Long press > Move Down | Move in hierarchy | moveDown() |

##### Property Types

| Type | Widget | Example |
|------|--------|---------|
| String | EditText | android:text |
| Int | EditText (numeric) | android:padding |
| Boolean | Switch | android:enabled |
| Color | ColorPicker | android:textColor |
| Dimension | EditText + unit spinner | android:layout_width |
| Enum | Spinner | android:visibility |
| Reference | AutoComplete | @drawable/ic_* |

#### Bottom Panel: Console

| ID | Tab | Content | Implementation |
|----|-----|---------|----------------|
| B1 | Console | General output | ConsolePanel("Console") |
| B2 | Build | Gradle output | ConsolePanel("Build") |
| B3 | Logcat | Android logs | LogcatPanel |
| B4 | Problems | Errors/warnings | ProblemsPanel |
| B5 | Terminal | Shell access | TerminalView |

##### Console Actions

| ID | Action | Function | Implementation |
|----|--------|----------|----------------|
| BC1 | Clear | Clear output | console.clear() |
| BC2 | Copy All | Copy all text | clipboard.setText(console.getText()) |
| BC3 | Scroll to Bottom | Auto-scroll | console.scrollToEnd() |
| BC4 | Filter | Filter by level | console.setFilter(level) |
| BC5 | Export | Save to file | console.exportToFile() |

##### Logcat Specific

| ID | Action | Function | Implementation |
|----|--------|----------|----------------|
| BL1 | Package Filter | Filter by app | logcat.setPackage(pkg) |
| BL2 | Level Filter | Filter by level | logcat.setLevel(level) |
| BL3 | Search | Search in logs | logcat.search(query) |
| BL4 | Clear | Clear logs | adb logcat -c |
| BL5 | Pause/Resume | Toggle updates | logcat.togglePause() |

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+N | New file |
| Ctrl+O | Open file |
| Ctrl+S | Save file |
| Ctrl+Shift+S | Save all |
| Ctrl+W | Close tab |
| Ctrl+Shift+W | Close all tabs |
| Ctrl+Z | Undo |
| Ctrl+Shift+Z | Redo |
| Ctrl+F | Find |
| Ctrl+H | Find and replace |
| Ctrl+G | Go to line |
| Ctrl+B | Build |
| Ctrl+R | Run |
| Ctrl+. | Quick fix |
| F1 | Claude help |
| F5 | Build and run |
| F6 | Build only |

### Dialogs

#### New Project Dialog

| Field | Type | Required | Default | Validation |
|-------|------|----------|---------|------------|
| Project Name | EditText | Yes | - | ^[a-zA-Z][a-zA-Z0-9_]*$ |
| Package Name | EditText | Yes | com.example.myapp | Valid Java package |
| Save Location | PathPicker | Yes | ~/projects/mobile/ | Writable directory |
| Min SDK | Spinner | Yes | 24 | API level |
| Language | Spinner | Yes | Kotlin | Kotlin/Java |
| Template | Spinner | No | Empty Activity | Template list |

#### New File Dialog

| Field | Type | Options |
|-------|------|---------|
| File Name | EditText | - |
| File Type | Spinner | Kotlin, Java, XML Layout, XML Values, Drawable |
| Location | PathPicker | Current or select |

---

## Room 2: Web Dev

**Priority:** #2
**Target Users:** Frontend developers
**Core Value:** Live preview web development

### Complete UI Element List

#### Top Toolbar

| ID | Element | Label | Function | Implementation |
|----|---------|-------|----------|----------------|
| T1 | btn_menu | ≡ | Navigation drawer | DrawerLayout.openDrawer() |
| T2 | room_title | "Web Dev" | Display name | Static |
| T3 | btn_new_project | + New | Create web project | showNewWebProjectDialog() |
| T4 | btn_open | Open | Open project | showProjectPicker() |
| T5 | file_tabs | HTML/CSS/JS | Switch file type | tabLayout.selectTab() |
| T6 | btn_save | 💾 | Save current file | save() |
| T7 | btn_run | ▶ Server | Start local server | startServer() |
| T8 | btn_stop | ⏹ | Stop server | stopServer() |
| T9 | btn_preview | 👁 | Toggle preview | togglePreview() |
| T10 | btn_settings | ⚙ | Settings | showSettings() |
| T11 | btn_claude | Claude | Toggle Claude | toggleClaude() |

#### Left Panel: Files

| ID | Element | Function | Implementation |
|----|---------|----------|----------------|
| L1 | file_tree | Project files | FileTreeAdapter |
| L2 | btn_add_html | + HTML | Create HTML file | createFile(".html") |
| L3 | btn_add_css | + CSS | Create CSS file | createFile(".css") |
| L4 | btn_add_js | + JS | Create JS file | createFile(".js") |
| L5 | btn_import | Import | Import assets | showImportDialog() |

#### Center Panel: Editor + Preview

| ID | Element | Function | Implementation |
|----|---------|----------|----------------|
| C1 | tab_bar | Open files | TabBar |
| C2 | code_editor | Code editing | CodeEditorView |
| C3 | preview_webview | Live preview | WebView |
| C4 | split_toggle | Toggle split | Button |

##### Preview Controls

| ID | Element | Function | Implementation |
|----|---------|----------|----------------|
| P1 | device_mobile | Phone viewport | setViewport(375, 667) |
| P2 | device_tablet | Tablet viewport | setViewport(768, 1024) |
| P3 | device_desktop | Desktop viewport | setViewport(1920, 1080) |
| P4 | device_custom | Custom size | showSizeDialog() |
| P5 | btn_refresh | Force reload | preview.reload() |
| P6 | btn_devtools | Toggle console | toggleDevTools() |

#### Right Panel: Styles & Elements

| ID | Element | Function | Implementation |
|----|---------|----------|----------------|
| R1 | element_tree | DOM tree | DomTreeAdapter |
| R2 | computed_styles | CSS properties | StylesAdapter |
| R3 | box_model | Visual box model | BoxModelView |

#### Bottom Panel

| ID | Tab | Content | Implementation |
|----|-----|---------|----------------|
| B1 | Console | Browser console | ConsolePanel |
| B2 | Network | HTTP requests | NetworkPanel |
| B3 | Elements | DOM inspector | ElementsPanel |
| B4 | Server | Server logs | ConsolePanel |
| B5 | Terminal | Shell | TerminalView |

### Special Features

#### Emmet Support

| Abbreviation | Expansion |
|--------------|-----------|
| ! | HTML5 boilerplate |
| div.class | `<div class="class"></div>` |
| ul>li*5 | 5 list items |
| #id | `<div id="id"></div>` |

#### Auto-Reload

| Trigger | Action |
|---------|--------|
| File save | Refresh preview |
| CSS change | Hot reload styles |
| JS change | Full reload |

---

## Room 3: Terminal

**Priority:** #3
**Target Users:** Power users
**Core Value:** Raw Claude Code access

### Complete UI Element List

#### Top Toolbar

| ID | Element | Label | Function | Implementation |
|----|---------|-------|----------|----------------|
| T1 | btn_menu | ≡ | Navigation drawer | DrawerLayout.openDrawer() |
| T2 | room_title | "Terminal" | Display name | Static |
| T3 | btn_new_session | + New | New terminal session | createSession() |
| T4 | session_tabs | [1] [2] [3] | Session switcher | TabLayout |
| T5 | btn_fullscreen | ⛶ | Toggle fullscreen | toggleFullscreen() |
| T6 | btn_settings | ⚙ | Terminal settings | showSettings() |

#### Main Area

| ID | Element | Function | Implementation |
|----|---------|----------|----------------|
| M1 | terminal_view | Terminal display | TerminalView |
| M2 | extra_keys | Special keys row | ExtraKeysView |

#### Extra Keys Row

| Key | Function | Sends |
|-----|----------|-------|
| ESC | Escape | \x1b |
| TAB | Tab | \t |
| CTRL | Control modifier | - |
| ALT | Alt modifier | - |
| ↑ | Arrow up | \x1b[A |
| ↓ | Arrow down | \x1b[B |
| ← | Arrow left | \x1b[D |
| → | Arrow right | \x1b[C |
| - | Dash | - |
| / | Slash | / |
| ≡ | Context menu | - |

#### Session Tab Actions

| Action | Trigger | Function |
|--------|---------|----------|
| Switch | Tap | Switch to session |
| Rename | Long press > Rename | Set session name |
| Close | Long press > Close | Kill session |
| Duplicate | Long press > Duplicate | Clone session |

#### Settings

| Setting | Type | Default | Options |
|---------|------|---------|---------|
| Font Size | Slider | 14 | 8-32 |
| Font Family | Spinner | Monospace | List |
| Color Scheme | Spinner | Dark | Dark/Light/Custom |
| Cursor Style | Spinner | Block | Block/Bar/Underline |
| Cursor Blink | Switch | On | On/Off |
| Bell | Spinner | Vibrate | None/Vibrate/Sound |
| Keyboard | Spinner | Default | Default/Custom |

---

## Room 4: Data Science

**Priority:** #4
**Target Users:** Data analysts, Python developers
**Core Value:** Python notebooks, data visualization

### Complete UI Element List

#### Top Toolbar

| ID | Element | Label | Function |
|----|---------|-------|----------|
| T1 | btn_menu | ≡ | Navigation drawer |
| T2 | room_title | "Data Science" | Display name |
| T3 | btn_new_notebook | + Notebook | Create notebook |
| T4 | btn_new_script | + Script | Create .py file |
| T5 | btn_open | Open | Open file |
| T6 | file_tabs | Notebook/Script | Switch mode |
| T7 | btn_run_all | ▶▶ | Run all cells |
| T8 | btn_stop | ⏹ | Stop kernel |
| T9 | btn_restart | ↻ | Restart kernel |
| T10 | btn_settings | ⚙ | Settings |
| T11 | btn_claude | Claude | Claude panel |

#### Left Panel: Files & Variables

| ID | Element | Function |
|----|---------|----------|
| L1 | file_tree | Project files |
| L2 | variable_explorer | Python variables |
| L3 | data_preview | DataFrame preview |

#### Center Panel: Notebook

| ID | Element | Function |
|----|---------|----------|
| C1 | cell_list | Notebook cells |
| C2 | cell_toolbar | Cell actions |
| C3 | cell_output | Execution output |

##### Cell Types

| Type | Icon | Function |
|------|------|----------|
| Code | { } | Python code |
| Markdown | M↓ | Documentation |
| Raw | R | Plain text |

##### Cell Actions

| Action | Button | Function |
|--------|--------|----------|
| Run | ▶ | Execute cell |
| Add Above | + ↑ | Insert cell above |
| Add Below | + ↓ | Insert cell below |
| Delete | 🗑 | Delete cell |
| Move Up | ↑ | Move cell up |
| Move Down | ↓ | Move cell down |
| Copy | 📋 | Copy cell |
| Cut | ✂ | Cut cell |
| Paste | 📋+ | Paste cell |

#### Right Panel: Inspector

| ID | Element | Function |
|----|---------|----------|
| R1 | data_info | DataFrame info |
| R2 | column_list | Column details |
| R3 | statistics | Basic stats |
| R4 | plot_preview | Chart thumbnail |

#### Bottom Panel

| Tab | Content |
|-----|---------|
| Console | IPython console |
| Plots | Generated charts |
| Data | Table view |
| Terminal | Shell |

### Data Visualization

| Chart Type | Library | Function |
|------------|---------|----------|
| Line | matplotlib | plt.plot() |
| Bar | matplotlib | plt.bar() |
| Scatter | matplotlib | plt.scatter() |
| Histogram | matplotlib | plt.hist() |
| Heatmap | seaborn | sns.heatmap() |
| Interactive | plotly | px.line() |

---

## Room 5: API/Backend

**Priority:** #5
**Target Users:** Backend developers
**Core Value:** REST API testing, database management

### Complete UI Element List

#### Top Toolbar

| ID | Element | Label | Function |
|----|---------|-------|----------|
| T1 | btn_menu | ≡ | Navigation drawer |
| T2 | room_title | "API Backend" | Display name |
| T3 | mode_tabs | Requests/Database/Server | Mode switcher |
| T4 | btn_new_request | + Request | New API request |
| T5 | btn_import | Import | Import collection |
| T6 | btn_export | Export | Export collection |
| T7 | btn_settings | ⚙ | Settings |
| T8 | btn_claude | Claude | Claude panel |

#### Left Panel: Collections

| ID | Element | Function |
|----|---------|----------|
| L1 | collection_tree | Saved requests |
| L2 | environment_picker | Select environment |
| L3 | history_list | Recent requests |

#### Center Panel: Request Builder

| ID | Element | Function |
|----|---------|----------|
| C1 | method_picker | GET/POST/PUT/DELETE/etc |
| C2 | url_input | Request URL |
| C3 | btn_send | Send request |
| C4 | request_tabs | Params/Headers/Body/Auth |

##### Request Tabs

| Tab | Content |
|-----|---------|
| Params | Query parameters |
| Headers | Request headers |
| Body | Request body (JSON/Form/Raw) |
| Auth | Authentication settings |
| Pre-request | Pre-request script |
| Tests | Post-request tests |

#### Right Panel: Response

| ID | Element | Function |
|----|---------|----------|
| R1 | status_badge | HTTP status |
| R2 | time_display | Response time |
| R3 | size_display | Response size |
| R4 | response_tabs | Body/Headers/Cookies |
| R5 | body_viewer | JSON viewer |

#### Bottom Panel

| Tab | Content |
|-----|---------|
| Console | Request/response logs |
| Environment | Variable values |
| History | Request history |
| Terminal | Shell |

### Environment Variables

| Scope | Example |
|-------|---------|
| Global | {{base_url}} |
| Environment | {{api_key}} |
| Collection | {{collection_var}} |

### Authentication Types

| Type | Fields |
|------|--------|
| None | - |
| Basic | Username, Password |
| Bearer | Token |
| API Key | Key, Value, Location |
| OAuth 2.0 | Grant type, URLs, Client ID/Secret |

---

## Room 6: AI/ML

**Priority:** #6
**Target Users:** ML engineers, AI enthusiasts
**Core Value:** AI model interaction, prompt engineering

### Complete UI Element List

#### Top Toolbar

| ID | Element | Label | Function |
|----|---------|-------|----------|
| T1 | btn_menu | ≡ | Navigation drawer |
| T2 | room_title | "AI/ML" | Display name |
| T3 | mode_tabs | Chat/Prompts/Models | Mode switcher |
| T4 | model_picker | Model selector | Choose AI model |
| T5 | btn_new_chat | + Chat | New conversation |
| T6 | btn_settings | ⚙ | Settings |
| T7 | btn_api_keys | 🔑 | API key management |

#### Left Panel: Prompts & History

| ID | Element | Function |
|----|---------|----------|
| L1 | prompt_library | Saved prompts |
| L2 | chat_history | Past conversations |
| L3 | template_list | Prompt templates |

#### Center Panel: Chat

| ID | Element | Function |
|----|---------|----------|
| C1 | message_list | Conversation view |
| C2 | system_prompt | System message editor |
| C3 | user_input | Message input |
| C4 | btn_send | Send message |
| C5 | btn_attach | Attach file |

##### Message Actions

| Action | Function |
|--------|----------|
| Copy | Copy message |
| Regenerate | Regenerate response |
| Edit | Edit and resend |
| Delete | Remove message |

#### Right Panel: Model Info

| ID | Element | Function |
|----|---------|----------|
| R1 | model_info | Model details |
| R2 | token_counter | Token usage |
| R3 | cost_estimate | Cost display |
| R4 | parameters | Model parameters |

##### Parameters

| Parameter | Type | Range |
|-----------|------|-------|
| Temperature | Slider | 0.0 - 2.0 |
| Max Tokens | Number | 1 - 128000 |
| Top P | Slider | 0.0 - 1.0 |
| Frequency Penalty | Slider | -2.0 - 2.0 |
| Presence Penalty | Slider | -2.0 - 2.0 |

#### Bottom Panel

| Tab | Content |
|-----|---------|
| Console | API logs |
| Tokens | Token breakdown |
| Export | Save conversation |
| Terminal | Shell |

### Supported Models

| Provider | Models |
|----------|--------|
| Anthropic | Claude 3.5, Claude 3, Claude 2 |
| OpenAI | GPT-4, GPT-3.5 |
| Local | Ollama models |

### API Key Storage

| Key | Encrypted | Location |
|-----|-----------|----------|
| Anthropic | Yes | EncryptedSharedPreferences |
| OpenAI | Yes | EncryptedSharedPreferences |
| Custom | Yes | EncryptedSharedPreferences |

---

## Navigation Drawer (All Rooms)

| ID | Item | Function |
|----|------|----------|
| N1 | Home | Return to room picker |
| N2 | Recent Projects | List of recent projects |
| N3 | Room: Mobile Apps | Switch to Mobile Apps |
| N4 | Room: Web Dev | Switch to Web Dev |
| N5 | Room: Terminal | Switch to Terminal |
| N6 | Room: Data Science | Switch to Data Science |
| N7 | Room: API/Backend | Switch to API/Backend |
| N8 | Room: AI/ML | Switch to AI/ML |
| N9 | Settings | Global settings |
| N10 | About | App info |
| N11 | Help | Documentation |

---

## Global Settings

| Category | Setting | Type | Default |
|----------|---------|------|---------|
| Appearance | Theme | Enum | Dark |
| Appearance | Font Size | Int | 14 |
| Appearance | Font Family | Enum | Monospace |
| Editor | Auto Save | Bool | true |
| Editor | Auto Format | Bool | false |
| Editor | Tab Size | Int | 4 |
| Editor | Word Wrap | Bool | true |
| Terminal | Default Shell | Enum | bash |
| Terminal | Cursor Style | Enum | Block |
| Build | Java Home | Path | auto |
| Build | Android SDK | Path | ~/android-sdk |
| Build | Gradle Daemon | Bool | false |
| AI | Default Model | Enum | Claude 3.5 |
| AI | API Key (Claude) | String | encrypted |
| AI | API Key (OpenAI) | String | encrypted |

---

## Total Element Count

| Room | Buttons | Panels | Features | Total |
|------|---------|--------|----------|-------|
| Mobile Apps | 47 | 4 | 32 | 83 |
| Web Dev | 38 | 4 | 24 | 66 |
| Terminal | 18 | 2 | 12 | 32 |
| Data Science | 42 | 4 | 28 | 74 |
| API/Backend | 45 | 4 | 30 | 79 |
| AI/ML | 35 | 4 | 22 | 61 |
| Navigation | 11 | 1 | 0 | 12 |
| Settings | 15 | 0 | 0 | 15 |
| **TOTAL** | **251** | **23** | **148** | **422** |

---

**Document Status:** Complete
**Last Updated:** January 7, 2026
