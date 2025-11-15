package com.maintenance.maintenance.model.dto;

import com.maintenance.maintenance.model.entity.Machine;
import java.util.ArrayList;
import java.util.List;

public class StockParCategorie {
    private Long categoryId;
    private String categoryName;
    private String categoryDescription;
    private Integer nombreMachinesOperationnelles;
    private Integer nombreMachinesTotal;
    private Integer nombreMachinesSecours; // Nombre de machines de secours
    private Integer nombreMachinesNeuves; // Matériels neufs en programmation
    private Integer nombreMachinesDisponibles; // Machines opérationnelles et non en réparation
    private Integer nombreMachinesNonOperationnelles; // Non opérationnelles ou indisponibles
    private Integer nombreMachinesEnReparation;
    private List<Machine> machines = new ArrayList<>();

    public StockParCategorie() {
        this.nombreMachinesOperationnelles = 0;
        this.nombreMachinesTotal = 0;
        this.nombreMachinesSecours = 0;
        this.nombreMachinesNeuves = 0;
        this.nombreMachinesDisponibles = 0;
        this.nombreMachinesNonOperationnelles = 0;
        this.nombreMachinesEnReparation = 0;
    }

    public StockParCategorie(Long categoryId, String categoryName, String categoryDescription) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryDescription = categoryDescription;
        this.nombreMachinesOperationnelles = 0;
        this.nombreMachinesTotal = 0;
        this.nombreMachinesSecours = 0;
        this.nombreMachinesNeuves = 0;
        this.nombreMachinesDisponibles = 0;
        this.nombreMachinesNonOperationnelles = 0;
        this.nombreMachinesEnReparation = 0;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDescription() {
        return categoryDescription;
    }

    public void setCategoryDescription(String categoryDescription) {
        this.categoryDescription = categoryDescription;
    }

    public Integer getNombreMachinesOperationnelles() {
        return nombreMachinesOperationnelles != null ? nombreMachinesOperationnelles : 0;
    }

    public void setNombreMachinesOperationnelles(Integer nombreMachinesOperationnelles) {
        this.nombreMachinesOperationnelles = nombreMachinesOperationnelles;
    }

    public Integer getNombreMachinesTotal() {
        return nombreMachinesTotal != null ? nombreMachinesTotal : 0;
    }

    public void setNombreMachinesTotal(Integer nombreMachinesTotal) {
        this.nombreMachinesTotal = nombreMachinesTotal;
    }

    public List<Machine> getMachines() {
        return machines;
    }

    public void setMachines(List<Machine> machines) {
        this.machines = machines;
    }

    public Integer getNombreMachinesSecours() {
        return nombreMachinesSecours != null ? nombreMachinesSecours : 0;
    }

    public void setNombreMachinesSecours(Integer nombreMachinesSecours) {
        this.nombreMachinesSecours = nombreMachinesSecours;
    }

    public Integer getNombreMachinesNeuves() {
        return nombreMachinesNeuves != null ? nombreMachinesNeuves : 0;
    }

    public void setNombreMachinesNeuves(Integer nombreMachinesNeuves) {
        this.nombreMachinesNeuves = nombreMachinesNeuves;
    }

    public Integer getNombreMachinesDisponibles() {
        return nombreMachinesDisponibles != null ? nombreMachinesDisponibles : 0;
    }

    public void setNombreMachinesDisponibles(Integer nombreMachinesDisponibles) {
        this.nombreMachinesDisponibles = nombreMachinesDisponibles;
    }

    public Integer getNombreMachinesNonOperationnelles() {
        return nombreMachinesNonOperationnelles != null ? nombreMachinesNonOperationnelles : 0;
    }

    public void setNombreMachinesNonOperationnelles(Integer nombreMachinesNonOperationnelles) {
        this.nombreMachinesNonOperationnelles = nombreMachinesNonOperationnelles;
    }

    public Integer getNombreMachinesEnReparation() {
        return nombreMachinesEnReparation != null ? nombreMachinesEnReparation : 0;
    }

    public void setNombreMachinesEnReparation(Integer nombreMachinesEnReparation) {
        this.nombreMachinesEnReparation = nombreMachinesEnReparation;
    }

    public void addMachine(Machine machine) {
        this.machines.add(machine);

        boolean enReparation = machine.getEnReparation() != null && machine.getEnReparation();
        boolean operationnel = machine.getOperationnel() != null && machine.getOperationnel();
        boolean estNeuve = machine.getEstMachineEntrepot() != null && machine.getEstMachineEntrepot();

        boolean disponible = !enReparation && operationnel;
        boolean compterDansTotal = estNeuve || disponible;

        if (compterDansTotal) {
            this.nombreMachinesTotal = getNombreMachinesTotal() + 1;
        }

        if (disponible) {
            this.nombreMachinesOperationnelles = getNombreMachinesOperationnelles() + 1;
            this.nombreMachinesDisponibles = getNombreMachinesDisponibles() + 1;
        } else if (!estNeuve) {
            this.nombreMachinesNonOperationnelles = getNombreMachinesNonOperationnelles() + 1;
        }

        if (enReparation) {
            this.nombreMachinesEnReparation = getNombreMachinesEnReparation() + 1;
        }

        if (machine.getEstMachineSecours() != null && machine.getEstMachineSecours()) {
            this.nombreMachinesSecours = getNombreMachinesSecours() + 1;
        }
        if (estNeuve) {
            this.nombreMachinesNeuves = getNombreMachinesNeuves() + 1;
        }
    }
}

