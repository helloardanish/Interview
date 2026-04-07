package com.ar.Interview.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class Employee {
    private int id;
    private String name;
    private int age;
    private String department;
    private String address;
    private double salary;
    private String gender;
}
