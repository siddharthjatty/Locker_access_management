package com.tt.auth.entity;

import com.tt.auth.entity.enums.AllocationStatus;
import com.tt.auth.entity.enums.LockerSize;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "locker_allocation_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockerAllocationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_size", nullable = false)
    private LockerSize requestedSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "forwarded_to_admin", columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean forwardedToAdmin = false;

    @Column(name = "forwarded_at")
    private LocalDateTime forwardedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
