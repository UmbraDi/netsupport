package com.egs.netsupport.web;

import com.egs.netsupport.diagnostic.SubscriberDiagnostic;
import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.monitoring.VlanService;
import com.egs.netsupport.shared.model.Device;
import com.egs.netsupport.shared.repository.DeviceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DeviceRepository deviceRepository;
    private final SubscriberDiagnostic subscriberDiagnostic;
    private final VlanService vlanService;

    public ApiController(DeviceRepository deviceRepository,
                         SubscriberDiagnostic subscriberDiagnostic,
                         VlanService vlanService) {
        this.deviceRepository = deviceRepository;
        this.subscriberDiagnostic = subscriberDiagnostic;
        this.vlanService = vlanService;
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return deviceRepository.findAllCities();
    }

    @GetMapping("/houses")
    public List<String> houses(@RequestParam(required = false) String city) {
        return deviceRepository.findHouses(city);
    }

    @GetMapping("/diagnose")
    public DiagnosticResult diagnose(@RequestParam String house,
                                     @RequestParam String mac) {
        return subscriberDiagnostic.diagnose(house, mac);
    }

    @GetMapping("/vlans")
    public List<HouseVlanDto> vlans() {
        return vlanService.getHouseVlans();
    }

}