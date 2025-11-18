package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

public class Main {

    // Настройки подключения (подправь под свою БД/пользователя)
    private static final String DB_URL  =
            "jdbc:mysql://localhost:3306/string_menu?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private static Connection connection;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            System.out.println("Подключение к MySQL установлено.");

            runMenu();

        } catch (SQLException e) {
            System.out.println("Ошибка подключения к БД: " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("Подключение к БД закрыто.");
                }
            } catch (SQLException e) {
                System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
    }

    private static void runMenu() {
        while (true) {
            printMenu();
            int choice = readInt("Выберите пункт меню: ");

            try {
                switch (choice) {
                    case 1 -> showTables();
                    case 2 -> createTableIfNotExists();
                    case 3 -> insertTwoStrings();
                    case 4 -> calculateLengths();
                    case 5 -> concatenateStrings();
                    case 6 -> compareStrings();
                    case 7 -> exportToExcel();
                    case 0 -> {
                        System.out.println("Выход из программы.");
                        return;
                    }
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (SQLException | IOException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n===== МЕНЮ (строковый тип + MySQL + Excel) =====");
        System.out.println("1.  Вывести все таблицы из MySQL.");
        System.out.println("2.  Создать таблицу в MySQL.");
        System.out.println("3.  Ввести две строки с клавиатуры, результат сохранить в MySQL с последующим выводом в консоль.");
        System.out.println("4.  Подсчитать размер ранее введенных строк, результат сохранить в MySQL с последующим выводом в консоль.");
        System.out.println("5.  Объединить две строки в единое целое, результат сохранить в MySQL с последующим выводом в консоль.");
        System.out.println("6.  Сравнить две ранее введенные строки, результат сохранить в MySQL с последующим выводом в консоль.");
        System.out.println("7.  Сохранить все данные из MySQL в Excel и вывести на экран.");
        System.out.println("0.  Выход.");
    }

    // ---------- helpers ----------

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
    }

    private static String readNonEmptyLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (!line.isBlank()) {
                return line;
            }
            System.out.println("Строка не должна быть пустой.");
        }
    }

    // ---------- пункт 1: показать все таблицы ----------

    private static void showTables() throws SQLException {
        String sql = "SHOW TABLES";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nСписок таблиц в текущей БД:");
            boolean any = false;
            while (rs.next()) {
                String tableName = rs.getString(1);
                System.out.println("- " + tableName);
                any = true;
            }
            if (!any) {
                System.out.println("(таблиц нет)");
            }
        }
    }

    // ---------- пункт 2: создать таблицу ----------

    private static void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS string_results (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    str1 TEXT NOT NULL,
                    str2 TEXT NOT NULL,
                    len1 INT,
                    len2 INT,
                    concat_str TEXT,
                    compare_result VARCHAR(50),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Таблица string_results создана (или уже существовала).");
        }
    }

    // ---------- пункт 3: ввод двух строк и сохранение ----------

    private static void insertTwoStrings() throws SQLException {
        // Требование: не менее 50 символов каждая
        String s1 = readStringWithMinLength("Введите первую строку (>= 50 символов): ");
        String s2 = readStringWithMinLength("Введите вторую строку (>= 50 символов): ");

        String sql = "INSERT INTO string_results (str1, str2) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s1);
            ps.setString(2, s2);
            ps.executeUpdate();

            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }

            System.out.println("\nСтроки успешно сохранены в БД.");
            System.out.println("ID записи: " + id);
            System.out.println("str1: " + s1);
            System.out.println("str2: " + s2);
        }
    }

    private static String readStringWithMinLength(String prompt) {
        while (true) {
            String s = readNonEmptyLine(prompt);
            if (s.length() >= 50) {
                return s;
            }
            System.out.println("Строка должна быть не короче 50 символов. Сейчас: " + s.length());
        }
    }

    // ---------- получить ID последней записи ----------

    private static Integer getLastId() throws SQLException {
        String sql = "SELECT id FROM string_results ORDER BY id DESC LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return null;
    }

    // ---------- пункт 4: посчитать длину строк ----------

    private static void calculateLengths() throws SQLException {
        Integer id = getLastId();
        if (id == null) {
            System.out.println("В таблице нет ни одной строки. Сначала выполните пункт 3.");
            return;
        }

        String selectSql = "SELECT str1, str2 FROM string_results WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Запись с id=" + id + " не найдена.");
                    return;
                }

                String s1 = rs.getString("str1");
                String s2 = rs.getString("str2");
                int len1 = s1.length();
                int len2 = s2.length();

                String updateSql = "UPDATE string_results SET len1 = ?, len2 = ? WHERE id = ?";
                try (PreparedStatement upd = connection.prepareStatement(updateSql)) {
                    upd.setInt(1, len1);
                    upd.setInt(2, len2);
                    upd.setInt(3, id);
                    upd.executeUpdate();
                }

                System.out.println("\nДлины строк (ID=" + id + "):");
                System.out.println("str1: " + s1);
                System.out.println("len1 = " + len1);
                System.out.println("str2: " + s2);
                System.out.println("len2 = " + len2);
            }
        }
    }

    // ---------- пункт 5: объединить строки ----------

    private static void concatenateStrings() throws SQLException {
        Integer id = getLastId();
        if (id == null) {
            System.out.println("В таблице нет ни одной строки. Сначала выполните пункт 3.");
            return;
        }

        String selectSql = "SELECT str1, str2 FROM string_results WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Запись с id=" + id + " не найдена.");
                    return;
                }

                String s1 = rs.getString("str1");
                String s2 = rs.getString("str2");
                String concat = s1 + s2;

                String updateSql = "UPDATE string_results SET concat_str = ? WHERE id = ?";
                try (PreparedStatement upd = connection.prepareStatement(updateSql)) {
                    upd.setString(1, concat);
                    upd.setInt(2, id);
                    upd.executeUpdate();
                }

                System.out.println("\nРезультат объединения строк (ID=" + id + "):");
                System.out.println("str1: " + s1);
                System.out.println("str2: " + s2);
                System.out.println("concat_str: " + concat);
            }
        }
    }

    // ---------- пункт 6: сравнить две строки ----------

    private static void compareStrings() throws SQLException {
        Integer id = getLastId();
        if (id == null) {
            System.out.println("В таблице нет ни одной строки. Сначала выполните пункт 3.");
            return;
        }

        String selectSql = "SELECT str1, str2 FROM string_results WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Запись с id=" + id + " не найдена.");
                    return;
                }

                String s1 = rs.getString("str1");
                String s2 = rs.getString("str2");

                int cmp = s1.compareTo(s2);
                String result;
                if (cmp == 0) {
                    result = "Строки равны";
                } else if (cmp < 0) {
                    result = "str1 < str2 (лексикографически)";
                } else {
                    result = "str1 > str2 (лексикографически)";
                }

                String updateSql = "UPDATE string_results SET compare_result = ? WHERE id = ?";
                try (PreparedStatement upd = connection.prepareStatement(updateSql)) {
                    upd.setString(1, result);
                    upd.setInt(2, id);
                    upd.executeUpdate();
                }

                System.out.println("\nРезультат сравнения строк (ID=" + id + "):");
                System.out.println("str1: " + s1);
                System.out.println("str2: " + s2);
                System.out.println("compare_result: " + result);
            }
        }
    }

    // ---------- пункт 7: экспорт в Excel ----------

    private static void exportToExcel() throws SQLException, IOException {
        String sql = "SELECT * FROM string_results ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("strings");

            // Заголовки
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("str1");
            header.createCell(2).setCellValue("str2");
            header.createCell(3).setCellValue("len1");
            header.createCell(4).setCellValue("len2");
            header.createCell(5).setCellValue("concat_str");
            header.createCell(6).setCellValue("compare_result");
            header.createCell(7).setCellValue("created_at");

            int rowIndex = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(rs.getInt("id"));
                row.createCell(1).setCellValue(rs.getString("str1"));
                row.createCell(2).setCellValue(rs.getString("str2"));

                int len1 = rs.getInt("len1");
                if (rs.wasNull()) {
                    row.createCell(3).setCellValue("");
                } else {
                    row.createCell(3).setCellValue(len1);
                }

                int len2 = rs.getInt("len2");
                if (rs.wasNull()) {
                    row.createCell(4).setCellValue("");
                } else {
                    row.createCell(4).setCellValue(len2);
                }

                row.createCell(5).setCellValue(rs.getString("concat_str"));
                row.createCell(6).setCellValue(rs.getString("compare_result"));
                Timestamp ts = rs.getTimestamp("created_at");
                row.createCell(7).setCellValue(
                        ts == null ? "" : ts.toString()
                );
            }

            // Немного автоширины колонок
            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            String fileName = "string_results.xlsx";
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                workbook.write(fos);
            }

            System.out.println("\nДанные успешно выгружены в файл: " + fileName);
            System.out.println("Откройте его в Excel и проверьте содержимое.");
        }
    }
}
