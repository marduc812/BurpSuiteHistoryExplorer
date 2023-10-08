package com.marduc812;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

public class HistoryExplorerGui extends JPanel {

    private final JCheckBox twoHunCheckBox;
    private final JCheckBox threeHunCheckBox;
    private final JCheckBox fourHunCheckBox;
    private final JCheckBox fiveHunCheckBox;
    private final JCheckBox regExCheckBox;
    private final JTextField includeExtensionsinput;
    private final JTextField excludeExtensionsinput;

    private final DefaultTableModel tableModel;

    public HistoryExplorerGui(MontoyaApi api) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));


        // BORDER VIEW
        Border roundedLineBorder = new javax.swing.border.LineBorder(Color.BLACK, 1, true);
        TitledBorder titledBorder = new TitledBorder(roundedLineBorder, "Search HTTP History");
        Border emptyBorder = new EmptyBorder(10, 10, 10, 10);
        Border compoundBorder = new CompoundBorder(titledBorder, emptyBorder);
        titledBorder.setTitleFont(titledBorder.getTitleFont().deriveFont(Font.BOLD));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(compoundBorder);


        // MAIN SEARCH
        JPanel searchInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JTextField searchInput = new JTextField("");
        JButton searchBtn = new JButton("Search");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.3); // 30% of screen width
        searchInput.setPreferredSize(new Dimension(width - 10, searchInput.getPreferredSize().height));
        searchInput.setBorder(BorderFactory.createCompoundBorder(
                searchInput.getBorder(),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));

        searchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput = searchInput.getText();
                boolean[] checkboxStates = getCheckboxStates();
                boolean regExSearch = regExCheckBox.isSelected();
                String includedExtensions = includeExtensionsinput.getText();
                String excludeExtensions = excludeExtensionsinput.getText();
                // Arguments:
                // api: burp api
                // userInput: text types by user
                // regExSearch: text or regex search
                // checkboxStates: Status code response: 200,300,400,500
                // includedExtensions: extensions to include
                // excludedExtensions: extensions to exclude
                new HistoryExplorer(api, HistoryExplorerGui.this, userInput, regExSearch, checkboxStates, includedExtensions, excludeExtensions);
            }
        });

        searchInputPanel.add(searchInput);
        searchInputPanel.add(searchBtn);


        // REGEX LAYOUT
        JPanel regexOptionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        regExCheckBox = new JCheckBox("RegEx Search");
        regexOptionsPanel.add(regExCheckBox);

        // STATUS RESPONSE CODES LAYOUT
        JPanel searchOptionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel optionsLabel = new JLabel("Status filter:");
        twoHunCheckBox = new JCheckBox("2XX", true);
        threeHunCheckBox = new JCheckBox("3XX", true);
        fourHunCheckBox = new JCheckBox("4XX", true);
        fiveHunCheckBox = new JCheckBox("5XX", true);


        searchOptionsPanel.add(optionsLabel);
        searchOptionsPanel.add(twoHunCheckBox);
        searchOptionsPanel.add(threeHunCheckBox);
        searchOptionsPanel.add(fourHunCheckBox);
        searchOptionsPanel.add(fiveHunCheckBox);

        // EXTENSION CODE LAYOUT
        JPanel searchExtensionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel includeExtensionsLabel = new JLabel("Include extensions: ");
        JLabel excludeExtensionsLabel = new JLabel("Exclude extensions: ");


        includeExtensionsinput = new JTextField("");
        includeExtensionsinput.setPreferredSize(new Dimension(300,includeExtensionsinput.getPreferredSize().height));
        excludeExtensionsinput = new JTextField("");
        excludeExtensionsinput.setPreferredSize(new Dimension(300,excludeExtensionsinput.getPreferredSize().height));


        JButton helpBtn = new JButton("?");

        JFrame frame = new JFrame("Help Window");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());

        helpBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Display a simple help message using JOptionPane
                JOptionPane.showMessageDialog(frame, "Extensions should be comma separated values. If you want to include/exclude requests without extensions you can use the \"none\" keyword. ", "Help window", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        searchExtensionPanel.add(includeExtensionsLabel);
        searchExtensionPanel.add(includeExtensionsinput);
        searchExtensionPanel.add(excludeExtensionsLabel);
        searchExtensionPanel.add(excludeExtensionsinput);
        searchExtensionPanel.add(helpBtn);


        // OUTPUT

        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        String[] columnNames = {"Host", "Output"};

        Map<String, String> dataMap = new LinkedHashMap<>();

        String[][] data = new String[dataMap.size()][2];
        int index = 0;
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            data[index][0] = entry.getKey();
            data[index][1] = entry.getValue();
            index++;
        }


        tableModel = new DefaultTableModel(data, columnNames);
        JTable table = new JTable(tableModel);

        int tableWidth = (int) (screenSize.width * 0.8);
        int tableHeight = (int) (screenSize.height * 0.5);

        // Add JTable to JScrollPane
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(tableWidth, tableHeight));
        outputPanel.add(scrollPane);

        // RESIZE THE VIEWS
        searchInputPanel.setMaximumSize(searchInputPanel.getPreferredSize());
        regexOptionsPanel.setMaximumSize(regexOptionsPanel.getPreferredSize());
        searchOptionsPanel.setMaximumSize(searchOptionsPanel.getPreferredSize());
        searchExtensionPanel.setMaximumSize(searchExtensionPanel.getPreferredSize());

        // ADD EVERY VIEW

        // Create the box layout
        mainPanel.add(searchInputPanel);
        mainPanel.add(regexOptionsPanel);
        mainPanel.add(searchOptionsPanel);
        mainPanel.add(searchExtensionPanel);

        // Add every view
        add(mainPanel);
        add(outputPanel);
    }

    private boolean[] getCheckboxStates() {
        return new boolean[]{
                twoHunCheckBox.isSelected(),
                threeHunCheckBox.isSelected(),
                fourHunCheckBox.isSelected(),
                fiveHunCheckBox.isSelected()
        };
    }

    public void updateTableData(Map<String, String> newData) {
        // Clear the existing data
        tableModel.setRowCount(0);

        // Add the new data
        for (Map.Entry<String, String> entry : newData.entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
    }
}
