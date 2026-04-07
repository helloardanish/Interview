package com.ar.Interview.stream;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Generate {

    private static final String[] NAMES = { "Amit", "Rahul", "Priya", "Neha", "Vikram", "Anjali", "Mohan", "Ratan",
            "Raj" };
    private static final String[] DEPARTMENTS = { "IT", "HR", "Finance", "Sales" };
    private static final String[] EMAIL_DOMAINS = { "gmail.com", "yahoo.com", "company.com" };

    public static List<Employee> getEmployees(int count) {
        List<Employee> allEmployees = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            String name = randomFrom(NAMES);
            String dept = randomFrom(DEPARTMENTS);
            int age = ThreadLocalRandom.current().nextInt(20, 60);
            double salary = ThreadLocalRandom.current().nextDouble(20000, 150000);

            String email = name.toLowerCase() + i + "@" + randomFrom(EMAIL_DOMAINS);

            allEmployees.add(new Employee(
                    i,
                    name,
                    age,
                    dept,
                    email,
                    salary,
                    UUID.randomUUID().toString()));
        }

        return allEmployees;
    }

    private static String randomFrom(String[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
}