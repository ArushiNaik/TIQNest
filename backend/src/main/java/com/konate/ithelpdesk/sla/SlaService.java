package com.konate.ithelpdesk.sla;

import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.Mapper.TicketMapper;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    @Transactional(readOnly = true)
    public SlaSummaryResponse summary() {
        Map<SlaStatus, Long> counts = ticketRepository.findAll().stream()
                .map(SlaPolicy::statusOf)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        long onTrack = counts.getOrDefault(SlaStatus.ON_TRACK, 0L);
        long atRisk = counts.getOrDefault(SlaStatus.AT_RISK, 0L);
        long breached = counts.getOrDefault(SlaStatus.BREACHED, 0L);
        long met = counts.getOrDefault(SlaStatus.MET, 0L);
        return new SlaSummaryResponse(onTrack, atRisk, breached, met, onTrack + atRisk + breached + met);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> alerts() {
        List<Ticket> urgent = ticketRepository.findAll().stream()
                .filter(t -> t.getResolvedAt() == null)
                .filter(t -> {
                    SlaStatus s = SlaPolicy.statusOf(t);
                    return s == SlaStatus.AT_RISK || s == SlaStatus.BREACHED;
                })
                .sorted(Comparator.comparing(Ticket::getSlaDeadline))
                .toList();
        return ticketMapper.toResponseList(urgent);
    }
}
