package com.ar.Interview.stream;

import java.util.*;
import java.util.stream.Collectors;

public class Client {

    public static void main(String[] args) {

        List<Employee> allEmployees = Generate.getEmployees(10);

        // 1. Employees whose name starts with A
        printEmployees(
                allEmployees.stream()
                        .filter(emp -> emp.getName() != null && emp.getName().startsWith("A"))
                        .toList());

        // 2. Group employees by department
        Map<String, List<Employee>> deptMap = allEmployees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment));

        // 3. Total count of employees
        System.out.println(allEmployees.size());

        // 4. Max age
        allEmployees.stream()
                .mapToInt(Employee::getAge)
                .max()
                .ifPresent(System.out::println);

        // 5. All department names
        List<String> deptNames = allEmployees.stream()
                .map(Employee::getDepartment)
                .distinct()
                .toList();
        System.out.println(deptNames);

        // 6. Count of employees in each department
        Map<String, Long> deptCountMap = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.counting()));

        // 7. Employees age < 30
        printEmployees(
                allEmployees.stream()
                        .filter(emp -> emp.getAge() < 30)
                        .toList());

        // 8. Employees age between 26 and 31
        printEmployees(
                allEmployees.stream()
                        .filter(emp -> emp.getAge() > 26 && emp.getAge() < 31)
                        .toList());

        // 9. Average age by gender
        Map<String, Double> avgAgeMap = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getGender,
                        Collectors.averagingInt(Employee::getAge)));

        // 10. Department with max employees
        Map.Entry<String, Long> deptMaxCount = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        System.out.println(deptMaxCount);

        // 11. Employees in Delhi sorted by name
        List<Employee> delhiEmployees = allEmployees.stream()
                .filter(emp -> "Delhi".equals(emp.getAddress()))
                .sorted(Comparator.comparing(Employee::getName))
                .toList();

        printEmployees(delhiEmployees);

        // 12. Average salary per department
        Map<String, Double> deptAvgSalary = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.averagingDouble(Employee::getSalary)));

        // 13. Highest salary in each department
        Map<String, Optional<Employee>> highestSalForEachDept = allEmployees.stream()
                .collect(Collectors.groupingBy(
                        Employee::getDepartment,
                        Collectors.maxBy(Comparator.comparing(Employee::getSalary))));

        // 14. Sort employees by salary
        List<Employee> sortedBySalary = allEmployees.stream()
                .sorted(Comparator.comparing(Employee::getSalary))
                .toList();

        printEmployees(sortedBySalary);

        // 15. Second lowest salary employee
        allEmployees.stream()
                .sorted(Comparator.comparing(Employee::getSalary))
                .skip(1)
                .findFirst()
                .ifPresent(System.out::println);
    }

    private static void printEmployees(List<Employee> employees) {
        employees.forEach(System.out::println);
    }
}