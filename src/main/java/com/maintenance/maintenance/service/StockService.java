package com.maintenance.maintenance.service;

import com.maintenance.maintenance.model.dto.StockParCategorie;
import com.maintenance.maintenance.model.entity.Category;
import com.maintenance.maintenance.model.entity.Machine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private MachineService machineService;

    @Autowired
    private CategoryService categoryService;

    /**
     * Calcule le stock par catégorie (nombre de machines opérationnelles)
     */
    public List<StockParCategorie> calculerStockParCategorie(String entrepriseId) throws Exception {
        // Récupérer toutes les machines de l'entreprise
        List<Machine> machines = machineService.listMachines(entrepriseId);
        
        // Récupérer toutes les catégories
        List<Category> categories = categoryService.findAll();
        
        // Créer un map pour stocker le stock par catégorie
        Map<Long, StockParCategorie> stockMap = new HashMap<>();
        
        // Initialiser toutes les catégories
        for (Category category : categories) {
            StockParCategorie stock = new StockParCategorie(
                category.getId(),
                category.getName(),
                category.getDescription()
            );
            stockMap.put(category.getId(), stock);
        }
        
        // Compter les machines par catégorie
        for (Machine machine : machines) {
            if (machine.getCategoryId() != null) {
                StockParCategorie stock = stockMap.get(machine.getCategoryId());
                if (stock != null) {
                    stock.addMachine(machine);
                } else {
                    // Catégorie non trouvée, créer une entrée
                    stock = new StockParCategorie(
                        machine.getCategoryId(),
                        machine.getCategoryName() != null ? machine.getCategoryName() : "Catégorie " + machine.getCategoryId(),
                        machine.getCategoryDescription()
                    );
                    stock.addMachine(machine);
                    stockMap.put(machine.getCategoryId(), stock);
                }
            }
        }
        
        // Filtrer les catégories vides (sans machines) et retourner la liste triée par nom de catégorie
        List<StockParCategorie> result = new ArrayList<>();
        for (StockParCategorie stock : stockMap.values()) {
            // Ne garder que les catégories qui ont au moins une machine
            if (stock.getNombreMachinesTotal() > 0) {
                result.add(stock);
            }
        }
        result.sort((a, b) -> {
            if (a.getCategoryName() == null) return 1;
            if (b.getCategoryName() == null) return -1;
            return a.getCategoryName().compareToIgnoreCase(b.getCategoryName());
        });
        
        return result;
    }

    /**
     * Récupère les machines d'une catégorie spécifique
     */
    public List<Machine> getMachinesParCategorie(String entrepriseId, Long categoryId) throws Exception {
        List<Machine> allMachines = machineService.listMachines(entrepriseId);
        List<Machine> machinesCategorie = new ArrayList<>();
        
        for (Machine machine : allMachines) {
            if (machine.getCategoryId() != null && machine.getCategoryId().equals(categoryId)) {
                machinesCategorie.add(machine);
            }
        }
        
        return machinesCategorie;
    }
}

