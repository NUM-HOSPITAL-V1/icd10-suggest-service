package mn.num.hospital.icd10_service.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mn.num.hospital.icd10_service.domain.Chapter;
import mn.num.hospital.icd10_service.dto.DiagnosisResponse;
import mn.num.hospital.icd10_service.dto.ICD10CodeDTO;
import mn.num.hospital.icd10_service.repo.ChapterRepository;
import mn.num.hospital.icd10_service.service.DiagnosisService;

/**
 * ICD-10 оношийн хайлтын UI цонх.
 *
 * Зүүн тал  : Chapter → Category → Subcategory шүүлтүүр + хайлтын талбар
 * Баруун тал : хайлтын үр дүн (карт жагсаалт)
 */
public class DiagnosisSearchUI {
	
    private static final Color BG = new Color(0xF7F8FA);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER_CLR = new Color(0xDDE1E7);
    private static final Color TEXT_DARK = new Color(0x1A1D23);
    private static final Color TEXT_GRAY = new Color(0x8A919E);
    private static final Color TEXT_MED = new Color(0x4A5568);
    private static final Color BADGE_BG = new Color(0x7A8CA0);
    private static final Color BADGE_TEXT = Color.WHITE;
    private static final Color HOVER_BG = new Color(0xEEF2FF);
    
    private static final Color CLOSE_BTN = new Color(0xF1F5F9);
    private static final Color CLOSE_HOV = new Color(0xFEE2E2);
    private static final Color CLOSE_FG = new Color(0x475569);
    private static final Color CLOSE_FGH = new Color(0xEF4444);

    private static final Color ACCENT = new Color(0x0EA5E9);
    private static final Color ACCENT_HOVER = new Color(0x0284C7);
    
    private static final String PLACEHOLDER = "Гэдэсний тодорхойгүй вирус халд...";
    private static final int DEFAULT_LIMIT = 50;

    private static final Logger log = LoggerFactory.getLogger(DiagnosisSearchUI.class);

    // UI компонентууд 
    private JFrame  frame;
    private JTextField searchField;
    private JComboBox<ComboItem> chapterCombo;
    private JComboBox<ComboItem> categoryCombo;
    private JComboBox<ComboItem> subCombo;
    private JPanel resultPanel;
    private JLabel countLabel;
    private JScrollPane scrollPane;

    // Сонгогдсон шүүлтүүрийн утгууд
    private String selectedChapter  = "";
    private String selectedCategory = "";

    // Dependency 
    private final DiagnosisService diagnosisService;
    private final ChapterRepository chapterRepo;

    public DiagnosisSearchUI(DiagnosisService diagnosisService,
                             ChapterRepository chapterRepo) {
        this.diagnosisService = diagnosisService;
        this.chapterRepo = chapterRepo;
        buildUI();
    }

    /** Үндсэн цонх*/
    private void buildUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        frame = new JFrame("Онош хайх");
        frame.setSize(920, 660);
        frame.setMinimumSize(new Dimension(1100, 860));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(WHITE);
        frame.setLayout(new BorderLayout());

        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildSplitPane(), BorderLayout.CENTER);
        frame.add(buildFooter(), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    /** Гарчгийн хэсэг (цонхны дээд тал). */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_CLR),
                new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel title = new JLabel("Онош хайх");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_DARK);
        header.add(title, BorderLayout.WEST);
        return header;
    }

    /** Зүүн, баруун хэсгийг хуваах JSplitPane. */
    private JSplitPane buildSplitPane() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(260);
        split.setDividerSize(1);
        split.setBorder(null);
        split.setResizeWeight(0.0);
        split.setLeftComponent(buildLeftPanel());
        split.setRightComponent(buildRightPanel());
        return split;
    }

    /** Зүүн панел: Combo шүүлтүүрүүд, хайлтын талбар, "Хайх" товч. */
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, BORDER_CLR),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Combo-уудыг үүсгэж эхний хоосон утга нэмэх
        chapterCombo = styledCombo();
        categoryCombo = styledCombo();
        subCombo = styledCombo();

        // Category, Subcategory-г эхэндээ идэвхгүй болгох (chapter сонгох хүртэл)
        categoryCombo.setEnabled(false);
        subCombo.setEnabled(false);

        chapterCombo.addItem(new ComboItem("", ""));
        categoryCombo.addItem(new ComboItem("", ""));
        subCombo.addItem(new ComboItem("", ""));

        panel.add(comboGroup("Ангилал", chapterCombo));
        panel.add(Box.createVerticalStrut(14));
        panel.add(comboGroup("Бүлэг", categoryCombo));
        panel.add(Box.createVerticalStrut(14));
        panel.add(comboGroup("Хэсэг", subCombo));
        panel.add(Box.createVerticalStrut(20));

        // Хуваагч шугам
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_CLR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(20));

        // Хайлтын талбар
        JLabel searchLbl = new JLabel("Оношны нэр эсвэл код");
        searchLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchLbl.setForeground(TEXT_MED);
        searchLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(searchLbl);
        panel.add(Box.createVerticalStrut(6));

        searchField = buildSearchField();
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(8));

        // "Хайх" товч
        JButton searchBtn = buildSearchButton();
        panel.add(searchBtn);
        panel.add(Box.createVerticalStrut(14));

        JLabel hint = new JLabel(
                "<html><span style='color:#8A919E;font-size:11px;'>"
                + "Оношны нэрийг эсвэл кодыг ойролцоогоор оруулна уу.</span></html>"
        );
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(hint);
        panel.add(Box.createVerticalGlue());

        // Listener-уудыг холбох
        wireComboListeners();

        // DB-с chapter жагсаалтыг ачааллана
        SwingUtilities.invokeLater(this::loadChapters);

        return panel;
    }

    /** Хайлтын текст талбарыг тохируулна. Enter дарахад хайлт хийнэ. */
    private JTextField buildSearchField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_DARK);
        field.setBackground(WHITE);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        setPlaceholder(field, PLACEHOLDER);

        // Enter дарахад хайлт хийнэ
        field.addActionListener(e -> triggerSearch());
        return field;
    }

    /** "Хайх" товч үүсгэж буцаана. */
    private JButton buildSearchButton() {
        JButton btn = new JButton("Хайх");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setBorder(new EmptyBorder(10, 0, 10, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> triggerSearch());
        btn.addMouseListener(colorHover(btn, ACCENT, ACCENT_HOVER, Color.WHITE, Color.WHITE));
        return btn;
    }

    /**
     * Combo-уудын ActionListener-уудыг холбоно.
     *
     * - Chapter сонгоход  → Category-г DB-с татна, Subcategory-г цэвэрлэнэ
     * - Category сонгоход → Subcategory-г DB-с татна
     * - Subcategory сонгоход → хайлт автоматаар хийхгүй (товч дарах шаардлагатай)
     */
    private void wireComboListeners() {
        chapterCombo.addActionListener(e -> {
            ComboItem selected = (ComboItem) chapterCombo.getSelectedItem();
            selectedChapter  = selected != null ? selected.getValue() : "";
            selectedCategory = "";

            loadCategories(selectedChapter);
            loadSubcategories("");           // subcategory цэвэрлэнэ

            // chapter сонгосон л бол category combo идэвхтэй болно
            categoryCombo.setEnabled(!selectedChapter.isBlank());
        });

        categoryCombo.addActionListener(e -> {
            ComboItem selected = (ComboItem) categoryCombo.getSelectedItem();
            selectedCategory = selected != null ? selected.getValue() : "";

            loadSubcategories(selectedCategory);

            // category сонгосон л бол subcategory combo идэвхтэй болно
            subCombo.setEnabled(!selectedCategory.isBlank());
        });

        // Subcategory-д тусдаа listener шаардлагагүй 
        // хэрэглэгч "Хайх" товч дарж хайна
    }

    /** DB-с бүх chapter-уудыг татаж chapterCombo-д нэмнэ. */
    private void loadChapters() {
        new Thread(() -> {
            try {
                List<Chapter> chapters = chapterRepo.findAll();
                SwingUtilities.invokeLater(() -> {
                    chapterCombo.removeAllItems();
                    chapterCombo.addItem(new ComboItem("", ""));
                    for (Chapter ch : chapters) {
                        String value = ch.getChapter();                      // "I", "II" ...
                        String label = ch.getChapter() + " " + ch.getName();
                        chapterCombo.addItem(new ComboItem(value, label));
                    }
                });
            } catch (Exception e) {
                log.error("Chapter татахад алдаа: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Сонгогдсон chapter-т харгалзах category жагсаалтыг DB-с татна.
     * Chapter хоосон бол combo-г цэвэрлэж идэвхгүй болгоно.
     */
    private void loadCategories(String chapter) {
        new Thread(() -> {
            try {
                List<String> categories = chapter.isBlank()
                        ? List.of()
                        : diagnosisService.getCategoriesByChapter(chapter);

                SwingUtilities.invokeLater(() -> {
                    categoryCombo.removeAllItems();
                    categoryCombo.addItem(new ComboItem("", ""));

                    for (String cat : categories) {
                        if (cat == null || cat.isBlank()) continue;

                        // Query үр дүн "A00|A00-A09 Нэр" хэлбэртэй ирнэ → prefix-г хасах
                        String withoutPrefix = cat.contains("|")
                                ? cat.split("\\|", 2)[1]
                                : cat;

                        String[] parts = withoutPrefix.trim().split(" ", 2);
                        String value   = parts.length > 0 ? parts[0] : withoutPrefix; // "A00-A09"
                        String label   = withoutPrefix;

                        categoryCombo.addItem(new ComboItem(value, label));
                    }
                });
            } catch (Exception e) {
                log.error("Category татахад алдаа: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Сонгогдсон category-д харгалзах subcategory жагсаалтыг DB-с татна.
     * Category хоосон бол combo-г цэвэрлэж идэвхгүй болгоно.
     */
    private void loadSubcategories(String category) {
        new Thread(() -> {
            try {
                List<String> subs = category.isBlank()
                        ? List.of()
                        : diagnosisService.getSubcategoriesByCategory(category);

                SwingUtilities.invokeLater(() -> {
                    subCombo.removeAllItems();
                    subCombo.addItem(new ComboItem("", ""));

                    for (String sub : subs) {
                        if (sub == null || sub.isBlank()) continue;

                        String[] parts = sub.trim().split(" ", 2);
                        String value   = parts.length > 0 ? parts[0] : sub; // "A00"
                        String label   = sub;

                        subCombo.addItem(new ComboItem(value, label));
                    }

                    subCombo.setEnabled(!subs.isEmpty());
                });
            } catch (Exception e) {
                log.error("Subcategory татахад алдаа: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * "Хайх" товч болон Enter дарахад дуудагдах үндсэн хайлтын метод.
     *
     * Логик:
     *   1. Хайлтын текст ≥ 2 тэмдэгт -> текстээр хайна, combo-оор шүүнэ
     *   2. Хайлтын текст хоосон, combo сонгосон → combo утгаар хайна
     *   3. Хоёулаа хоосон → үр дүнг цэвэрлэнэ
     */
    private void triggerSearch() {
        String raw = searchField.getText().trim();

        // Placeholder текст байвал хоосон гэж тооцно
        String query = raw.equals(PLACEHOLDER) ? "" : raw;

        boolean hasText  = query.length() >= 2;
        boolean hasCombo = !selectedChapter.isBlank()
                        || !selectedCategory.isBlank()
                        || !getSelectedSubValue().isBlank();

        if (!hasText && !hasCombo) {
            clearResults();
            return;
        }

        // Текст байвал текстээр, үгүй бол combo-оор хайлтын утга тодорхойлно
        String searchTerm = hasText ? query : buildComboQuery();

        new Thread(() -> {
            // Limit-г 2 дахин өргөтгөж авна - combo шүүлт хийсний дараа таслана
            List<ICD10CodeDTO> all = fetchFromService(searchTerm, DEFAULT_LIMIT * 2);
            List<ICD10CodeDTO> filtered = hasCombo ? filterByCombo(all) : all;
            List<ICD10CodeDTO> limited = filtered.stream()
                    .limit(DEFAULT_LIMIT)
                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> renderResults(limited));
        }).start();
    }

    /**
     * Combo-уудаас хайлтад хэрэглэх түлхүүр утгыг буцаана.
     * Дэд төрөл → Төрөл → Ерөнхий төрөл гэсэн дарааллаар эрэмбэлнэ.
     */
    private String buildComboQuery() {
        ComboItem sub = (ComboItem) subCombo.getSelectedItem();
        ComboItem cat = (ComboItem) categoryCombo.getSelectedItem();
        ComboItem chap = (ComboItem) chapterCombo.getSelectedItem();

        if (sub  != null && !sub .getValue().isBlank()) return sub.getValue();
        if (cat  != null && !cat .getValue().isBlank()) return cat.getValue();
        if (chap != null && !chap.getValue().isBlank()) return chap.getValue();
        return "";
    }

    /**
     * Хайлтын үр дүнг сонгогдсон combo шүүлтүүрээр нэмж шүүнэ.
     * Chapter -> Category -> Subcategory дарааллаар нарийсгана.
     */
    private List<ICD10CodeDTO> filterByCombo(List<ICD10CodeDTO> data) {
        String chapter = getComboValue(chapterCombo);
        String category = getComboValue(categoryCombo);
        String sub = getComboValue(subCombo);

        return data.stream()
                .filter(dto -> {
                    // Chapter шүүлт
                    if (!chapter.isBlank()) {
                        if (dto.getChapterCode() == null
                                || !dto.getChapterCode().equals(chapter)) return false;
                    }
                    // Category шүүлт (range-р эхэлж байгаа эсэх)
                    if (!category.isBlank()) {
                        if (dto.getCategoryRange() == null
                                || !dto.getCategoryRange().startsWith(category)) return false;
                    }
                    // Subcategory шүүлт (code-оор эхэлж байгаа эсэх)
                    if (!sub.isBlank()) {
                        if (dto.getSubcategoryCode() == null
                                || !dto.getSubcategoryCode().startsWith(sub)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /** Баруун панел: тоолуур + хайлтын үр дүн жагсаалт. */
    private JPanel buildRightPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);

        // Тоолуур
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        topBar.setBackground(BG);
        topBar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        countLabel = new JLabel("Хайлтын илэрц: 0");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        countLabel.setForeground(TEXT_DARK);
        topBar.add(countLabel);
        wrapper.add(topBar, BorderLayout.NORTH);

        // Карт жагсаалт
        resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBackground(BG);
        resultPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(resultPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    /** Үр дүнгийн жагсаалтыг цэвэрлэж тоолуурыг тэглэнэ. */
    private void clearResults() {
        resultPanel.removeAll();
        countLabel.setText("Хайлтын илэрц: 0");
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    /** Хайлтын үр дүнг карт хэлбэрээр харуулна. */
    private void renderResults(List<ICD10CodeDTO> data) {
        resultPanel.removeAll();
        countLabel.setText("Хайлтын илэрц: " + data.size());

        for (ICD10CodeDTO dto : data) {
            resultPanel.add(buildResultCard(dto));
            resultPanel.add(Box.createVerticalStrut(6));
        }

        resultPanel.revalidate();
        resultPanel.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    /** Нэг ICD-10 кодын хайлтын карт. */
    private JPanel buildResultCard(ICD10CodeDTO dto) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_CLR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Зүүн: ICD код badge
        card.add(buildBadge(dto.getCode()), BorderLayout.WEST);

        // Дунд: нэр + дэлгэрэнгүй
        JPanel textArea = new JPanel();
        textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
        textArea.setBackground(WHITE);

        JTextArea nameLbl = new JTextArea(dto.getCode() + " - " + dto.getName());
        nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nameLbl.setForeground(TEXT_DARK);
        nameLbl.setLineWrap(true);
        nameLbl.setWrapStyleWord(true);
        nameLbl.setOpaque(false);
        nameLbl.setEditable(false);
        nameLbl.setBorder(null);
        textArea.add(nameLbl);

        if (dto.getDetail() != null && !dto.getDetail().isEmpty()) {
        	JTextArea detLbl = new JTextArea(dto.getDetail());
        	detLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        	detLbl.setForeground(TEXT_GRAY);
        	detLbl.setLineWrap(true);
        	detLbl.setWrapStyleWord(true);
        	detLbl.setOpaque(false);
        	detLbl.setEditable(false);
        	detLbl.setBorder(null);
            textArea.add(detLbl);
        }
        card.add(textArea, BorderLayout.CENTER);

        // Hover + click эффект
        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(HOVER_BG);
                textArea.setBackground(HOVER_BG);
            }
            public void mouseExited(MouseEvent e) {
                card.setBackground(WHITE);
                textArea.setBackground(WHITE);
            }
            public void mouseClicked(MouseEvent e) { onSelect(dto); }
        };
        card.addMouseListener(hover);
        textArea.addMouseListener(hover);

        return card;
    }

    /** ICD кодыг харуулах дугуйдсан badge панел. */
    private JPanel buildBadge(String code) {
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BADGE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new GridBagLayout());
        badge.setPreferredSize(new Dimension(58, 46));

        JLabel lbl = new JLabel("<html><center>" + code + "</center></html>");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(BADGE_TEXT);
        badge.add(lbl);
        return badge;
    }

    /** Доод хэсэг: "БОЛИХ" товч. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(WHITE);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_CLR));

        JButton closeBtn = new JButton("БОЛИХ");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setForeground(CLOSE_FG);
        closeBtn.setBackground(CLOSE_BTN);
        closeBtn.setBorder(new EmptyBorder(9, 24, 9, 24));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(colorHover(closeBtn, CLOSE_BTN, CLOSE_HOV, CLOSE_FG, CLOSE_FGH));
        closeBtn.addActionListener(e -> frame.dispose());
        footer.add(closeBtn);

        return footer;
    }

    /**
     * DiagnosisService-ээс хайлтын үр дүнг татна.
     * Алдаа гарвал хэрэглэгчид мэдэгдэж хоосон жагсаалт буцаана.
     */
    private List<ICD10CodeDTO> fetchFromService(String query, int limit) {
        List<ICD10CodeDTO> result = new ArrayList<>();
        try {
            DiagnosisResponse response = diagnosisService.suggestCodes(query, limit);

            if (response.isSuccess()) {
                for (ICD10CodeDTO dto : response.getSuggestions()) {
                    result.add(new ICD10CodeDTO(
                            dto.getCode(),
                            dto.getName(),
                            dto.getDetail(),
                            dto.getChapterCode(),
                            dto.getChapterName(),
                            dto.getCategoryRange(),
                            dto.getCategoryName(),
                            dto.getSubcategoryCode(),
                            dto.getSubcategoryName(),
                            dto.getRelevanceScore()
                    ));
                }
            } else {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(frame, response.getMessage()));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(frame, "Алдаа: " + e.getMessage()));
        }
        return result;
    }

    /** Онош сонгогдоход харуулах мэдэгдэл. */
    private void onSelect(ICD10CodeDTO dto) {
        JOptionPane.showMessageDialog(
                frame,
                "Сонгосон: " + dto.getCode() + " - " + dto.getName(),
                "Онош сонгогдлоо",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /** Тухайн combo-оос сонгогдсон утгыг буцаана; сонголт байхгүй бол хоосон мөр. */
    private String getComboValue(JComboBox<ComboItem> combo) {
        ComboItem item = (ComboItem) combo.getSelectedItem();
        return item != null ? item.getValue() : "";
    }

    /** Subcategory combo-оос сонгогдсон утгыг буцаана. */
    private String getSelectedSubValue() {
        return getComboValue(subCombo);
    }

    /**
     * Styled JComboBox үүсгэнэ.
     * Font, өнгө, хэмжээ, хүрээ нийтлэг тохиргоотой.
     */
    private JComboBox<ComboItem> styledCombo() {
        JComboBox<ComboItem> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(WHITE);
        combo.setForeground(TEXT_DARK);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        combo.setBorder(new LineBorder(BORDER_CLR, 1, true));
        return combo;
    }

    /** Label + combo-г нийлүүлсэн бүлэг панел үүсгэнэ. */
    private JPanel comboGroup(String labelText, JComboBox<ComboItem> combo) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(WHITE);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(lbl);
        group.add(Box.createVerticalStrut(5));

        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(combo);
        return group;
    }

    /**
     * JTextField-д placeholder текст тохируулна.
     * Focus авахад арилж, буцаж харагдана.
     */
    private void setPlaceholder(JTextField field, String placeholder) {
        field.setForeground(TEXT_GRAY);
        field.setText(placeholder);

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_DARK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(TEXT_GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }

//    /** Товчлуур дээр cursor хүрэх үед өнгийг солих hover эффект. */
//    private MouseAdapter hoverEffect(AbstractButton btn, Color normal, Color hover) {
//        return new MouseAdapter() {
//            public void mouseEntered(MouseEvent e) { btn.setBackground(hover);  }
//            public void mouseExited (MouseEvent e) { btn.setBackground(normal); }
//        };
//    }
    
    private MouseAdapter colorHover(AbstractButton btn,
            Color normBg, Color hovBg,
            Color normFg, Color hovFg) {
    	return new MouseAdapter() {
    		public void mouseEntered(MouseEvent e) {
    			btn.setBackground(hovBg); btn.setForeground(hovFg); btn.repaint();
    		}
    		public void mouseExited(MouseEvent e) {
    			btn.setBackground(normBg); btn.setForeground(normFg); btn.repaint();
    		}
    	};
}

}