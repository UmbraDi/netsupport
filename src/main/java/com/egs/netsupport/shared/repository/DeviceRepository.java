package com.egs.netsupport.shared.repository;

import com.egs.netsupport.shared.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Integer> {

    List<Device> findByTypeAndIpIsNotNullOrderByIp(String type);

    List<Device> findByTypeOrderByIp(String type);

    List<Device> findByTypeAndIp(String type, String ip);

    @Query("""
              select distinct concat(d.street, ' ', d.houseNum)
              from Device d
              where d.type = 'switch'
                and d.street is not null
                and d.houseNum is not null
              order by concat(d.street, ' ', d.houseNum)
              """)
    List<String> findAllHouses();
}
