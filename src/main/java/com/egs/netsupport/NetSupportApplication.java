package com.egs.netsupport;

import com.egs.netsupport.diagnostic.model.PortDetails;
import com.egs.netsupport.diagnostic.parser.FdbParser;
import com.egs.netsupport.diagnostic.parser.InterfaceParser;
import com.egs.netsupport.monitoring.PingService;
import com.egs.netsupport.shared.model.Node;
import com.egs.netsupport.shared.repository.NodeRepository;
import com.egs.netsupport.shared.telnet.TelnetService;

import java.util.List;

public class NetSupportApplication {
    public static void main(String[] args) throws InterruptedException {
        String output =
                "GigabitEthernet1/0/7 is up, line protocol is up (connected)\n" +
                        "  Hardware is Gigabit Ethernet, address is f47f.35d5.b507 (bia f47f.35d5.b507)\n" +
                        "  MTU 1500 bytes, BW 100000 Kbit/sec, DLY 100 usec,\n" +
                        "     reliability 255/255, txload 1/255, rxload 1/255\n" +
                        "  Encapsulation ARPA, loopback not set\n" +
                        "  Keepalive set (10 sec)\n" +
                        "  Full-duplex, 100Mb/s, media type is 10/100/1000BaseTX\n" +
                        "  input flow-control is off, output flow-control is unsupported\n" +
                        "  ARP type: ARPA, ARP Timeout 04:00:00\n" +
                        "  Last input never, output 00:00:05, output hang never\n" +
                        "  Last clearing of \"show interface\" counters never\n" +
                        "  Input queue: 0/75/0/0 (size/max/drops/flushes); Total output drops: 8199440\n" +
                        "  Queueing strategy: fifo\n" +
                        "  Output queue: 0/40 (size/max)\n" +
                        "  5 minute input rate 2000 bits/sec, 1 packets/sec\n" +
                        "  5 minute output rate 14000 bits/sec, 2 packets/sec\n" +
                        "     1772411009 packets input, 355154777383 bytes, 0 no buffer\n" +
                        "     Received 4648 broadcasts (4609 multicasts)\n" +
                        "     0 runts, 0 giants, 0 throttles\n" +
                        "     2 input errors, 0 CRC, 0 frame, 0 overrun, 0 ignored\n" +
                        "     0 watchdog, 4609 multicast, 0 pause input\n" +
                        "     0 input packets with dribble condition detected\n" +
                        "     3200607861 packets output, 4312220931899 bytes, 0 underruns\n" +
                        "     0 output errors, 0 collisions, 1 interface resets\n" +
                        "     0 unknown protocol drops\n" +
                        "     0 babbles, 0 late collision, 0 deferred\n" +
                        "     0 lost carrier, 0 no carrier, 0 pause output\n" +
                        "     0 output buffer failures, 0 output buffers swapped out\n";

        InterfaceParser parser = new InterfaceParser();
        PortDetails details  = parser.parse(output);

        System.out.println(details);
    }
}