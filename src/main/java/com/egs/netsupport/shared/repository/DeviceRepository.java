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
          select distinct concat(d.street, ' ', d.houseNum)
          from Device d
          where d.type = 'switch'
            and d.street is not null
            and d.houseNum is not null
            and (:city is null or d.city = :city)
          order by concat(d.street, ' ', d.houseNum)
          """)
    List<String> findHouses(@Param("city") String city);

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
