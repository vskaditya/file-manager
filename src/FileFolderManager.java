
  //  import com.opencsv.CSVReader;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook; // For .xlsx files
import org.apache.poi.hssf.usermodel.HSSFWorkbook; // For .xls files (optional, legacy format)



    public class FileFolderManager extends JFrame {
        private JTable fileTable;
        private DefaultTableModel fileTableModel;
        private ArrayList<File> selectedFiles = new ArrayList<>();
        private LinkedHashSet<String> selectedFileNames = new LinkedHashSet<>();

        private File selectedFolder;
        private JDateChooser startDateChooser;
        private JDateChooser endDateChooser;
        private JDateChooser createdDateChooser;
        private JDateChooser modifiedDateChooser;
        private JComboBox<String> fileTypeComboBox;
        private JRadioButton createdRadioButton;
        private JRadioButton modifiedRadioButton;

        public FileFolderManager() {
            setTitle("File and Folder Manager");
            setSize(800, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            // Setup Page 1
            setupPageOne();

            setVisible(true);
        }

        private void setupPageOne() {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel buttonPanel = new JPanel();

            //JButton addFileButton = new JButton("Add File");
            JButton addFolderButton = new JButton("Add Folder");
            JButton removeButton = new JButton("Remove");
            JButton nextButton = new JButton("Next");

            //buttonPanel.add(addFileButton);
            buttonPanel.add(addFolderButton);
            buttonPanel.add(removeButton);
            buttonPanel.add(nextButton);

            fileTableModel = new DefaultTableModel(new String[]{"File Name", "Size"}, 0);
            fileTable = new JTable(fileTableModel);

            //addFileButton.addActionListener(e -> addFile());
            addFolderButton.addActionListener(e -> addFolder());
            removeButton.addActionListener(e -> removeFile());
            nextButton.addActionListener(e -> setupPageTwo());

            panel.add(new JScrollPane(fileTable), BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(panel);
            revalidate();
        }

        private void addFile() {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".csv")) {
                    JOptionPane.showMessageDialog(this, "Please select a CSV file!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selectedFiles.add(file);
                String size=formatSize(file.length());
                fileTableModel.addRow(new Object[]{file.getName(), size,  1024});
            }
        }

        private void addFolder() {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File folder = folderChooser.getSelectedFile();

                System.out.println(calculateDirectorySize(folder));


//            selectedFolder = folder;
                if (containsCSVFile(folder)) {
                    selectedFolder = folder;
                } else {
                    JOptionPane.showMessageDialog(this, "The selected folder does not contain any CSV files.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                //this.addFilesFromDir(folder);
                String size=formatSize( calculateDirectorySize(folder));
                fileTableModel.addRow(new Object[]{folder.getName(), size});
            }
        }
        private void setupPageTwo() {
            this.selectedFiles.clear();
            this.selectedFileNames.clear();
            if (selectedFolder == null) {
                JOptionPane.showMessageDialog(this, "Please select a folder first!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JPanel panel = new JPanel(new BorderLayout());
            JPanel bottomPanel = new JPanel();

            // Create the JTree with a "CSV" root node
            JTree folderTree = createFolderTreeWithCSVRoot(selectedFolder);
            folderTree.setPreferredSize(new Dimension(250, 600));
            DefaultTableModel infoTableModel = new DefaultTableModel(new String[]{"File Name", "Size"}, 0);
            JTable infoTable = new JTable(infoTableModel);

            // Add tree selection listener
            folderTree.addTreeSelectionListener(e -> {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (node == null) return;

                FileNode fileNode = (FileNode) node.getUserObject();
                File selectedFile = fileNode.getFile();

                if (selectedFile.isDirectory()) {
                    // Get the CSV files in the selected folder
                    List<File> csvFiles = getCSVFiles(selectedFile);


                    //infoTableModel.setRowCount(0); // Clear the table

                    if (!csvFiles.isEmpty()) {
                        // Add CSV files to the table
                        for (File csvFile : csvFiles) {
                            //condition to check if it is already added or not

                            boolean notAlreadyAdded = this.selectedFileNames.add(csvFile.getAbsolutePath());
                            if(notAlreadyAdded) {
                                this.selectedFiles.add(csvFile);
                                infoTableModel.addRow(new Object[]{csvFile.getName(), formatSize(csvFile.length())});
                            }
                        }
                    } else {
                        // Show an error if no CSV files are found
                        JOptionPane.showMessageDialog(this, "The selected folder does not contain any CSV files.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            JButton nextButton = new JButton("Next");
            JButton backButton = new JButton("Back");

            // Add button actions
            nextButton.addActionListener(e -> setupPageThree()); // Proceed to page three
            backButton.addActionListener(e -> setupPageOne());  // Go back to page one

            // Populate bottom panel
            bottomPanel.add(backButton);
            bottomPanel.add(nextButton);
            JScrollPane treeScrollPane = new JScrollPane(folderTree);
            treeScrollPane.setPreferredSize(new Dimension(250, 600));

            // Populate main panel
            panel.add(new JScrollPane(folderTree), BorderLayout.WEST);
            panel.add(new JScrollPane(infoTable), BorderLayout.CENTER);
            panel.add(bottomPanel, BorderLayout.SOUTH);

            setContentPane(panel);
            revalidate();
        }
        private void addFilesFromDir(File csvDirectory) {
            if(!csvDirectory.isDirectory()) {
                return;
            }
            File[] csvFiles = csvDirectory.listFiles(new CsvFileNameFilter());
            this.selectedFiles.clear();
            this.selectedFiles.addAll(Arrays.asList(csvFiles));
        }
        private JTree createFolderTreeWithCSVRoot(File root) {
            DefaultMutableTreeNode csvRootNode = new DefaultMutableTreeNode("CSV"); // Root node named "CSV"
            DefaultMutableTreeNode actualRootNode = new DefaultMutableTreeNode(new FileNode(root));

            csvRootNode.add(actualRootNode); // Add the actual root as a child of "CSV"
            populateTree(root, actualRootNode);
            return new JTree(csvRootNode);
        }


        // Helper method to create a folder tree


        // Populate the folder tree
        private void populateTree(File file, DefaultMutableTreeNode node) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(f));
                    node.add(childNode);
                    if (f.isDirectory()) {
                        populateTree(f, childNode);
                    }
                }
            }
        }

        // Helper class to represent tree nodes
        class FileNode {
            private File file;

            public FileNode(File file) {
                this.file = file;
            }

            public File getFile() {
                return file;
            }

            @Override
            public String toString() {
                return file.getName(); // Show only the file or folder name
            }
        }

        // Check if a folder contains at least one CSV file
        private boolean containsCSVFile(File folder) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".csv")) {
                        return true; // Found a CSV file
                    } else if (file.isDirectory() && containsCSVFile(file)) {
                        return true; // Check subdirectories
                    }
                }
            }
            return false; // No CSV files found
        }

        // Calculate the size of a directory
        private long calculateDirectorySize(File folder) {
            long totalSize = 0;

            // Get all files and folders in the directory
            File[] files = folder.listFiles();

            if (files != null) {
                // Loop through each file or subdirectory
                for (File file : files) {
                    if (file.isDirectory()) {
                        // If it's a directory, call the method recursively
                        totalSize += calculateDirectorySize(file);
                    } else {
                        // If it's a file, add its size to the total
                        totalSize += file.length();
                    }
                }
            }

            return totalSize;
        }

        // Format size for display (e.g., KB, MB, GB)
        private static String formatSize(long sizeInBytes) {
            if (sizeInBytes < 1024) {
                return sizeInBytes + " bytes"; // Less than 1 KB, show in bytes
            } else if (sizeInBytes < 1024 * 1024) {
                return String.format("%.2f KB", sizeInBytes / 1024.0); // Less than 1 MB, show in KB
            } else if (sizeInBytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024)); // Less than 1 GB, show in MB
            } else {
                return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024)); // 1 GB or more, show in GB
            }
        }

        private List<File> getCSVFiles(File folder) {
            List<File> csvFiles = new ArrayList<>();
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".csv")) {
                        csvFiles.add(file);
                    } //else if (file.isDirectory()) {
                    // csvFiles.addAll(getCSVFiles(file)); // Check subdirectories recursively
                    // }
                }
            }
            return csvFiles;
        }





        private void removeFile() {
            int selectedRow = fileTable.getSelectedRow();
            if (selectedRow != -1) {
                // Remove from the selectedFiles list and the table
                selectedFiles.remove(selectedRow);
                fileTableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a file to remove.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }






        private void setupPageThree() {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel topPanel = new JPanel(new BorderLayout());
            JPanel bottomPanel = new JPanel(new FlowLayout());

            // Make sure there are files selected
            if (selectedFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No CSV files selected for conversion.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Table to display selected CSV files
            DefaultTableModel fileTableModel = new DefaultTableModel(new String[]{"File Name", "Size", "Creation Date", "Last Modified Date"}, 0);
            JTable fileTable = new JTable(fileTableModel);

            // Loop through selected files and add only CSV files to the table
            for (File file : selectedFiles) {
                if (file.isFile() && file.getName().endsWith(".csv")) { // Ensure it's a CSV file
                    fileTableModel.addRow(new Object[]{
                            file.getName(),
                            formatSize(file.length()), // Format file size
                            getFormattedDate(file.lastModified()), // Format creation or modification date
                            getFormattedDate(file.lastModified())  // Format last modified date
                    });
                }
            }

            // Dropdown for file conversion format
            JComboBox<String> formatComboBox = new JComboBox<>(new String[]{"PDF", "Excel", "Text"});
            JButton convertButton = new JButton("Convert All");
            JButton backButton = new JButton("Back");

            // Date pickers for filtering
            startDateChooser = new JDateChooser();
            endDateChooser = new JDateChooser();
            JButton applyDateFilterButton = new JButton("Apply Date Filter");

            // Apply Date Filter button action
            applyDateFilterButton.addActionListener(e -> {
                Date startDate = startDateChooser.getDate();
                Date endDate = endDateChooser.getDate();

                if (startDate == null || endDate == null) {
                    JOptionPane.showMessageDialog(this, "Please select both start and end dates.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                System.out.println("Start Date: " + new SimpleDateFormat("yyyy-MM-dd").format(startDate));
                System.out.println("End Date: " + new SimpleDateFormat("yyyy-MM-dd").format(endDate));

                fileTableModel.setRowCount(0); // Clear existing rows
                boolean filesFound = false;

                for (File csvFile : selectedFiles) {
                    if (csvFile.isFile() && csvFile.getName().endsWith(".csv")) {
                        try {
                            long creationTime = getFileCreationTime(csvFile);
                            System.out.println("File: " + csvFile.getName());
                            System.out.println("Creation Time (formatted): " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(creationTime)));

                            if (creationTime >= startDate.getTime() && creationTime <= endDate.getTime()) {
                                System.out.println("File matches the date range: " + csvFile.getName());
                                fileTableModel.addRow(new Object[]{
                                        csvFile.getName(),
                                        formatSize(csvFile.length()),
                                        getFormattedDate(creationTime),
                                        getFormattedDate(csvFile.lastModified())
                                });
                                filesFound = true;
                            } else {
                                System.out.println("File does not match the date range: " + csvFile.getName());
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Error retrieving file creation date: " + csvFile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                if (!filesFound) {
                    JOptionPane.showMessageDialog(this, "No files match the selected date range.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            convertButton.addActionListener(e -> {
                String selectedFormat = (String) formatComboBox.getSelectedItem();
                Date startDate = startDateChooser.getDate();
                Date endDate = endDateChooser.getDate();

                if (startDate == null || endDate == null) {
                    JOptionPane.showMessageDialog(this, "Please select both start and end dates.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean conversionDone = false;

                for (File csvFile : selectedFiles) {
                    if (csvFile.isFile() && csvFile.getName().endsWith(".csv")) { // Ensure it's a CSV file
                        try {
                            long creationTime = getFileCreationTime(csvFile);

                            // Convert the file only if its creation date is within the selected range
                            if (creationTime >= startDate.getTime() && creationTime <= endDate.getTime()) {
                                File convertedFile = convertFile(csvFile, selectedFormat);
                                if (convertedFile != null) {
                                    JOptionPane.showMessageDialog(this, "Converted: " + csvFile.getName() + " -> " + selectedFormat,
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                                    conversionDone = true;
                                } else {
                                    JOptionPane.showMessageDialog(this, "Failed to convert: " + csvFile.getName(),
                                            "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            } else {
                                JOptionPane.showMessageDialog(this, "File does not match the selected date range: " + csvFile.getName(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Error retrieving file creation date: " + csvFile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                if (!conversionDone) {
                    JOptionPane.showMessageDialog(this, "No files within the selected date range were converted.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "All eligible files converted successfully!", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            // Back Button Action
            backButton.addActionListener(e -> setupPageTwo());

            // Populate bottom panel
            bottomPanel.add(new JLabel("Start Date:"));
            bottomPanel.add(startDateChooser);
            bottomPanel.add(new JLabel("End Date:"));
            bottomPanel.add(endDateChooser);
            bottomPanel.add(applyDateFilterButton);
            bottomPanel.add(new JLabel("Select Format:"));
            bottomPanel.add(formatComboBox);
            bottomPanel.add(convertButton);
            bottomPanel.add(backButton);

            // Populate main panel
            topPanel.add(new JScrollPane(fileTable), BorderLayout.CENTER);
            panel.add(topPanel, BorderLayout.CENTER);
            panel.add(bottomPanel, BorderLayout.SOUTH);

            setContentPane(panel);
            revalidate();
            repaint();
        }
        private long getFileCreationTime(File file) throws IOException {
            Path path = file.toPath();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return attrs.creationTime().toMillis(); // Returns the creation time in milliseconds
        }






        // Conversion Logic
        private  File convertFile(File csvFile, String format) {
            try {
                String outputPath = System.getProperty("user.home") + "/Downloads/" +
                        csvFile.getName().replace(".csv", "." + format.toLowerCase());
                File outputFile = new File(outputPath);

                switch (format) {
                    case "PDF":
                        // Generate a proper PDF using Apache PDFBox
                        try (PDDocument document = new PDDocument()) {
                            PDPage page = new PDPage();
                            document.addPage(page);

                            PDPageContentStream contentStream = new PDPageContentStream(document, page);
                            contentStream.beginText();
                            contentStream.setFont(PDType1Font.HELVETICA, 12);
                            contentStream.newLineAtOffset(50, 750);

                            // Read CSV content and add each line to the PDF
                            try (BufferedReader reader = Files.newBufferedReader(csvFile.toPath())) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    // Sanitize the line by removing or replacing tab characters
                                    line = line.replace("\t", "    "); // Replacing tab with spaces
                                    contentStream.showText(line);
                                    contentStream.newLineAtOffset(0, -15);  // Move to the next line in the PDF
                                }
                            }

                            contentStream.endText();
                            contentStream.close();

                            document.save(outputFile);
                        }
                        break;

                    case "Excel":
                        // Generate an Excel file using Apache POI
                        Workbook workbook = new XSSFWorkbook();
                        Sheet sheet = workbook.createSheet("Sheet1");

                        // Read CSV content and populate Excel file
                        try (BufferedReader reader = Files.newBufferedReader(csvFile.toPath())) {
                            String line;
                            int rowNum = 0;
                            while ((line = reader.readLine()) != null) {
                                Row row = sheet.createRow(rowNum++);
                                String[] values = line.split(",");
                                for (int i = 0; i < values.length; i++) {
                                    row.createCell(i).setCellValue(values[i]);
                                }
                            }
                        }

                        // Write Excel to file
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            workbook.write(fos);
                        }
                        workbook.close();
                        break;

                    case "Text":
                        // Copy CSV content to a text file
                        Files.copy(csvFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported format: " + format);
                }

                return outputFile;
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();  // Consider logging this or showing to the user
                return null;
            }
        }


        // Display Converted File Data in Table
        private void displayConvertedFile(File file, JTable table) {
            try {
                DefaultTableModel tableModel = new DefaultTableModel();
                table.setModel(tableModel);

                // Assuming the file is a CSV or text file
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    String[] headers = null;
                    boolean isFirstLine = true;

                    while ((line = reader.readLine()) != null) {
                        String[] rowData = line.split(","); // Split CSV data by commas

                        if (isFirstLine) {
                            headers = rowData;
                            tableModel.setColumnIdentifiers(headers);
                            isFirstLine = false;
                        } else {
                            tableModel.addRow(rowData);
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Unable to read the converted file.", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        // Method to handle file conversion
        private void convertSelectedFile(JTable csvTable, String format) {
            int selectedRow = csvTable.getSelectedRow();
            if (selectedRow != -1) {
                String filePath = (String) csvTable.getValueAt(selectedRow, 1);
                File selectedFile = new File(filePath);

                // Placeholder for conversion logic
                JOptionPane.showMessageDialog(this, "Converting " + selectedFile.getName() + " to " + format);

                // TODO: Add actual conversion logic here for PDF, Excel, or Text
            } else {
                JOptionPane.showMessageDialog(this, "Please select a file to convert.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void handleNextPage() {
            // Handle logic when next is clicked (store selected filters, etc.)
            String fileType = (String) fileTypeComboBox.getSelectedItem();
            Date startDate = startDateChooser.getDate();
            Date endDate = endDateChooser.getDate();
            Date createdDate = createdDateChooser.getDate();
            Date modifiedDate = modifiedDateChooser.getDate();
            String dateType = createdRadioButton.isSelected() ? "Created Date" : "Modified Date";

            // Process the selected filters (e.g., filter files based on dates)
            System.out.println("File Type: " + fileType);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            System.out.println(dateType + ": " + (createdRadioButton.isSelected() ? createdDate : modifiedDate));

            // Show a new page or do further processing
        }

        private String getFormattedDate(long timestamp) {
            // Create a SimpleDateFormat instance for formatting dates
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            //System.out.println("File: " + csvFile.getName() + ", Creation Time: " + getFormattedDate(creationTime));

            // Convert the long timestamp into a Date object and format it
            return sdf.format(new Date(timestamp));
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(FileFolderManager::new);
        }
    }

