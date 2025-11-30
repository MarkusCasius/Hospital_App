package com.example.hospimanagmenetapp;

import android.content.Context;
import com.example.hospimanagmenetapp.data.entities.Appointment;
import com.example.hospimanagmenetapp.data.repo.AppointmentRepository;
import com.example.hospimanagmenetapp.domain.DetectScheduleConflictsUseCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DetectScheduleConflictsUseCaseTest {

    @Mock
    private Context mockContext;
    @Mock
    private AppointmentRepository mockRepo;

    private DetectScheduleConflictsUseCase useCase;

    @Before
    public void setUp() {
        useCase = new DetectScheduleConflictsUseCase(mockContext);
    }

    @Test
    public void hasConflict_whenNoOverlappingAppointments_returnsFalse() {
        // Arrange
        DetectScheduleConflictsUseCase localUseCase = new DetectScheduleConflictsUseCase(mockContext) {
            public boolean hasConflict(long clinicianId, long start, long end, long idToIgnore) {
                when(mockRepo.detectConflicts(eq(clinicianId), eq(start), eq(end), eq(idToIgnore)))
                        .thenReturn(Collections.emptyList()); // No conflicts found
                return !mockRepo.detectConflicts(clinicianId, start, end, idToIgnore).isEmpty();
            }
        };

        // Act
        boolean result = localUseCase.hasConflict(1L, 1000L, 2000L, 0L);

        // Assert
        assertFalse(result);
    }

    @Test
    public void hasConflict_whenOverlappingAppointmentExists_returnsTrue() {
        // Arrange
        DetectScheduleConflictsUseCase localUseCase = new DetectScheduleConflictsUseCase(mockContext) {
            public boolean hasConflict(long clinicianId, long start, long end, long idToIgnore) {
                when(mockRepo.detectConflicts(eq(clinicianId), eq(start), eq(end), eq(idToIgnore)))
                        .thenReturn(Collections.singletonList(new Appointment())); // One conflict found
                return !mockRepo.detectConflicts(clinicianId, start, end, idToIgnore).isEmpty();
            }
        };

        // Act
        boolean result = localUseCase.hasConflict(1L, 1500L, 2500L, 0L);

        // Assert
        assertTrue(result);
    }
}

