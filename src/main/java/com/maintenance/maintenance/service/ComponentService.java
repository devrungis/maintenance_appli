package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.entity.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComponentService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    public List<Component> listComponentsForMachine(String entrepriseId, String machineId) throws Exception {
        return firebaseRealtimeService.getComponentsForMachine(entrepriseId, machineId);
    }

    public List<Component> listComponentsForEnterprise(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getComponentsForEnterprise(entrepriseId);
    }

    public Map<String, Long> countComponentsByMachine(String entrepriseId) throws Exception {
        List<Component> components = listComponentsForEnterprise(entrepriseId);
        Map<String, Long> counts = new HashMap<>();
        if (components == null) {
            return counts;
        }
        for (Component component : components) {
            if (component == null) {
                continue;
            }
            String machineId = component.getMachineId();
            if (machineId == null || machineId.trim().isEmpty()) {
                continue;
            }
            counts.put(machineId, counts.getOrDefault(machineId, 0L) + 1L);
        }
        return counts;
    }

    public List<Component> listAvailableComponents(String entrepriseId) throws Exception {
        return firebaseRealtimeService.getAvailableComponents(entrepriseId);
    }

    public Component getComponent(String entrepriseId, String componentId) throws Exception {
        return firebaseRealtimeService.getComponentById(entrepriseId, componentId);
    }

    public String createComponent(String entrepriseId, Component component) throws Exception {
        return firebaseRealtimeService.createComponent(entrepriseId, component);
    }

    public void updateComponent(String entrepriseId, String componentId, Component component) throws Exception {
        firebaseRealtimeService.updateComponent(entrepriseId, componentId, component);
    }

    public void deleteComponent(String entrepriseId, String componentId) throws Exception {
        firebaseRealtimeService.deleteComponent(entrepriseId, componentId);
    }
}


