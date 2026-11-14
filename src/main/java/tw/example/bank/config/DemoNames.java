package tw.example.bank.config;

import java.util.List;

/**
 * Fixed names for demo accounts 001-100, so every clone of the project gets the same logins as
 * "示範帳戶與登入說明.md". Passwords are "<name>_123". 70 distinct names; none appears more than 3 times.
 */
final class DemoNames {
    static final List<String> CUSTOMERS = List.of(
            "Brian Kuo", "Henry Chiu", "Vivian Wang", "Irene Lai", "Cindy Chang", // 001-005
            "Peggy Tseng", "Amy Liu", "Joyce Kuo", "Joyce Chang", "Claire Chen", // 006-010
            "Jason Lai", "Claire Tsai", "Sophia Liao", "Wendy Yang", "Alex Wang", // 011-015
            "Steven Chou", "Amy Chou", "Sophia Liu", "Grace Liu", "Peggy Lin", // 016-020
            "Amy Hsu", "Iris Chou", "Jason Wang", "Joyce Huang", "Joyce Chang", // 021-025
            "Oscar Chen", "Tina Hung", "Jason Wang", "Tina Hsu", "Ivan Kuo", // 026-030
            "Amy Chou", "Jason Lai", "Leo Hung", "Ryan Lee", "Claire Tsai", // 031-035
            "Iris Huang", "Claire Chen", "Steven Liu", "Brian Kuo", "Annie Huang", // 036-040
            "Cindy Kuo", "Ivan Tseng", "Irene Yang", "Claire Hsieh", "Peggy Lee", // 041-045
            "Joyce Kuo", "Iris Huang", "Amy Liu", "Ivan Hsieh", "Alex Chang", // 046-050
            "Joyce Huang", "Vivian Chiu", "Steven Liu", "Andy Chiu", "Sophia Wang", // 051-055
            "Kevin Wu", "Claire Tsai", "Joyce Huang", "Vivian Chou", "Annie Yang", // 056-060
            "Alex Liao", "Sophia Liu", "Ryan Chang", "Peggy Tseng", "Ivan Hsieh", // 061-065
            "Eric Wang", "Irene Chiu", "Ethan Huang", "Ivan Liao", "Alex Liao", // 066-070
            "Joyce Lee", "Tina Hung", "Cindy Cheng", "Peggy Lee", "Peggy Lin", // 071-075
            "Eric Lee", "Tina Hung", "Daniel Wang", "Joyce Lee", "Amy Wang", // 076-080
            "Kevin Lee", "Jerry Cheng", "Wendy Huang", "Tina Chiu", "Kevin Liu", // 081-085
            "Amy Hsu", "Tina Liao", "Jerry Cheng", "Vivian Chang", "Alex Hsieh", // 086-090
            "Henry Lai", "Irene Chiu", "Irene Cheng", "Tina Hsu", "Joyce Tseng", // 091-095
            "Tina Chiu", "Ethan Liu", "Ethan Lai", "Claire Chen", "Annie Huang" // 096-100
    );

    private DemoNames() {}
}
