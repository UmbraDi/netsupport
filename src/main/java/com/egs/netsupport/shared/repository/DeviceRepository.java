package com.egs.netsupport.shared.repository;

import com.egs.netsupport.shared.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Integer> {

    List<Device> findByTypeAndIpIsNotNullOrderByIp(String type);

    List<Device> findByTypeOrderByIp(String type);

    List<Device> findByTypeAndIp(String type, String ip);

    @Query("""
          select distinct d.city
          from Device d
          where d.type = 'switch' and d.city is not null
          order by d.city
          """)
    List<String> findAllCities();

    @Query("""
          select distinct d.street
          from Device d
          where d.type = 'switch'
            and d.street is not null
            and (:city is null or d.city = :city)
          order by d.street
          """)
    List<String> findStreets(@Param("city") String city);

    @Query("""
          select distinct d.houseNum
          from Device d
          where d.type = 'switch'
            and d.street = :street
            and d.houseNum is not null
            and (:city is null or d.city = :city)
          order by d.houseNum
          """)
    List<String> findHouseNums(@Param("city") String city, @Param("street") String street);

    @Query("""
          select d
          from Device d
          where d.type = 'switch'
            and d.ip is not null
            and (:city is null or d.city = :city)
            and (:street is null or d.street = :street)
            and (:houseNum is null or d.houseNum = :houseNum)
          order by d.ip
          """)
    List<Device> findSwitches(@Param("city") String city,
                              @Param("street") String street,
                              @Param("houseNum") String houseNum);

    @Query("""
          select d.city, d.street, d.houseNum, d.vlan
          from Device d
          where d.type = 'switch'
            and d.street is not null
            and d.houseNum is not null
            and d.vlan is not null
          order by d.city, d.street, d.houseNum, d.vlan
          """)
    List<Object[]> findHouseVlanRows();
}
