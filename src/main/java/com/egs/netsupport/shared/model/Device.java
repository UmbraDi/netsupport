package com.egs.netsupport.shared.model;

import jakarta.persistence.*;

@Entity
@Table(name = "devices", schema = "net")
public class Device {

    @Id
    @Column(name = "id")
    public Integer id;

    @Column(name = "name")
    public String name;

    @Column(name = "ip")
    public String ip;

    @Column(name = "type")
    public String type;

    @Column(name = "vlan")
    public Integer vlan;

    @Column(name = "city")
    public String city;

    @Column(name = "street")
    public String street;

    @Column(name = "house_num")
    public String houseNum;
}
