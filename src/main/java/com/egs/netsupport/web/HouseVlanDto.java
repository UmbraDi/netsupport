package com.egs.netsupport.web;

import java.util.List;

public record HouseVlanDto(
        String city,
        String street,
        String houseNum,
        List<Integer> vlans
) {
    public String getHouse() {
        return street + " " + houseNum;
    }
}