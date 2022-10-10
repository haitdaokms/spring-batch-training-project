package com.kms.springbatchprj.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "customer")
@Entity
@Getter
@Setter
public class CustomerEntity {
    @Id
    @Column(name = "id")
    private String customerId;

    @Column(name = "firstname")
    private String firstName;

    @Column(name = "lastname")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "gender")
    private String gender;

    @Column(name = "contact")
    private String contactNo;

    @Column(name = "country")
    private String country;

    @Column(name = "dob")
    private String dob;
}
