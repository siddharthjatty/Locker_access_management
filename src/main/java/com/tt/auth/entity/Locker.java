package com.tt.auth.entity;

import com.tt.auth.entity.enums.LockerSize;
import com.tt.auth.entity.enums.LockerStatus;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "lockers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Locker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locker_number", nullable = false, unique = true)
    private String lockerNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LockerSize size;

    @Column(nullable = false)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LockerStatus status = LockerStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to")
    private Customer assignedTo;

    @Column(name = "assigned_at")
    private java.time.LocalDateTime assignedAt;

    @Column(name = "visit_count", columnDefinition = "int default 0")
    @Builder.Default
    private Integer visitCount = 0;
}
