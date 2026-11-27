package com.egs.netsupport.web;

import com.egs.netsupport.diagnostic.SubscriberDiagnostic;
import com.egs.netsupport.diagnostic.model.DiagnosticResult;
import com.egs.netsupport.monitoring.PortStatusService;
import com.egs.netsupport.monitoring.VlanService;
import com.egs.netsupport.monitoring.model.ErrDisabledDto;
import com.egs.netsupport.shared.repository.DeviceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DeviceRepository deviceRepository;
    private final SubscriberDiagnostic subscriberDiagnostic;
    private final VlanService vlanService;
    private final PortStatusService portStatusService;

    public ApiController(DeviceRepository deviceRepository,
                         SubscriberDiagnostic subscriberDiagnostic,
                         VlanService vlanService,
                         PortStatusService portStatusService) {
        this.deviceRepository = deviceRepository;
        this.subscriberDiagnostic = subscriberDiagnostic;
        this.vlanService = vlanService;
        this.portStatusService = portStatusService;
    }

    @GetMapping("/cities")
    public List<String> cities() {
        return deviceRepository.findAllCities();
    }

    @GetMapping("/streets")
    public List<String> streets(@RequestParam(required = false) String city) {
        return deviceRepository.findStreets(blankToNull(city));
    }

    @GetMapping("/house-nums")
    public List<String> houseNums(@RequestParam(required = false) String city,
                                  @RequestParam String street) {
        return deviceRepository.findHouseNums(blankToNull(city), street);
    }

    @GetMapping("/diagnose")
    public DiagnosticResult diagnose(@RequestParam(required = false) String city,
                                     @RequestParam String street,
                                     @RequestParam String houseNum,
                                     @RequestParam String mac) {
        return subscriberDiagnostic.diagnose(blankToNull(city), street, houseNum, mac);
    }

    @GetMapping("/vlans")
    public List<HouseVlanDto> vlans() {
        return vlanService.getHouseVlans();
    }

    /**
     * Поиск err-disabled портов на коммутаторах (по кнопке из веб-интерфейса).
     * Возвращает только коммутаторы, у которых есть порты в ошибке.
     */
    @GetMapping("/errors")
    public List<ErrDisabledDto> errors(@RequestParam(required = false) String city) {
        return portStatusService.findErrDisabled(blankToNull(city));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
