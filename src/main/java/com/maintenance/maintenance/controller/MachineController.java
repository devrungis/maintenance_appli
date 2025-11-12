package com.maintenance.maintenance.controller;

import com.maintenance.maintenance.model.dto.MachineForm;
import com.maintenance.maintenance.model.entity.Machine;
import com.maintenance.maintenance.service.FirebaseRealtimeService;
import com.maintenance.maintenance.service.MachineService;
import com.maintenance.maintenance.service.LocalFileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/machines")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private LocalFileStorageService localFileStorageService;

    private boolean ensureSuperAdmin(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            redirectAttributes.addFlashAttribute("error", "Session expirée. Merci de vous reconnecter.");
            return false;
        }
        String role = (String) session.getAttribute("role");
        if (!"superadmin".equalsIgnoreCase(role)) {
            redirectAttributes.addFlashAttribute("error", "Accès réservé au super administrateur.");
            return false;
        }
        return true;
    }

    @GetMapping
    public String listMachines(@RequestParam(value = "entrepriseId", required = false) String entrepriseId,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            if (enterprises == null) {
                enterprises = new ArrayList<>();
            }

            if (!StringUtils.hasText(entrepriseId) && !enterprises.isEmpty()) {
                entrepriseId = enterprises.get(0).get("entrepriseId").toString();
            }

            List<Machine> machines = new ArrayList<>();
            if (StringUtils.hasText(entrepriseId)) {
                machines = machineService.listMachines(entrepriseId);
            }

            model.addAttribute("enterprises", enterprises);
            model.addAttribute("selectedEntrepriseId", entrepriseId);
            model.addAttribute("machines", machines);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des machines: " + e.getMessage());
            return "redirect:/dashboard";
        }

        return "machines/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("entrepriseId") String entrepriseId,
                                 Model model,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            List<Map<String, Object>> enterprises = firebaseRealtimeService.getAllEnterprises();
            model.addAttribute("enterprises", enterprises);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement des entreprises: " + e.getMessage());
            return "redirect:/machines";
        }

        MachineForm machineForm = new MachineForm();
        machineForm.setEntrepriseId(entrepriseId);
        model.addAttribute("machineForm", machineForm);
        return "machines/create";
    }

    @PostMapping
    public String createMachine(@ModelAttribute("machineForm") MachineForm machineForm,
                                @RequestParam(value = "photoFiles", required = false) MultipartFile[] photoFiles,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!StringUtils.hasText(machineForm.getEntrepriseId())) {
            redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner une entreprise.");
            return "redirect:/machines";
        }
        if (!StringUtils.hasText(machineForm.getNom())) {
            redirectAttributes.addFlashAttribute("error", "Le nom de la machine est obligatoire.");
            return "redirect:/machines/create?entrepriseId=" + machineForm.getEntrepriseId();
        }

        try {
            Machine machine = machineForm.toMachine();
            machine.setPhotos(new ArrayList<>());

            String machineId = machineService.createMachine(machineForm.getEntrepriseId(), machine);

            List<String> uploadedPhotos = localFileStorageService.saveMachinePhotos(
                machineForm.getEntrepriseId(),
                machineId,
                photoFiles,
                3
            );

            if (!uploadedPhotos.isEmpty()) {
                machine.setPhotos(uploadedPhotos);
                machineService.updateMachine(machineForm.getEntrepriseId(), machineId, machine);
            }
            redirectAttributes.addFlashAttribute("success", "Machine créée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création de la machine: " + e.getMessage());
        }

        return "redirect:/machines?entrepriseId=" + machineForm.getEntrepriseId();
    }

    @GetMapping("/{entrepriseId}/{machineId}/edit")
    public String showEditForm(@PathVariable String entrepriseId,
                               @PathVariable String machineId,
                               Model model,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine machine = machineService.getMachine(entrepriseId, machineId);
            if (machine == null) {
                redirectAttributes.addFlashAttribute("error", "Machine introuvable.");
                return "redirect:/machines?entrepriseId=" + entrepriseId;
            }

            MachineForm form = new MachineForm();
            form.setEntrepriseId(entrepriseId);
            form.setNom(machine.getNom());
            form.setNumeroSerie(machine.getNumeroSerie());
            form.setExistingPhotos(machine.getPhotos());
            form.setEmplacement(machine.getEmplacement());
            form.setNotes(machine.getNotes());

            model.addAttribute("machineForm", form);
            model.addAttribute("machineId", machineId);
            model.addAttribute("entrepriseId", entrepriseId);
            model.addAttribute("existingPhotos", machine.getPhotos());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors du chargement de la machine: " + e.getMessage());
            return "redirect:/machines?entrepriseId=" + entrepriseId;
        }

        return "machines/edit";
    }

    @PostMapping("/{entrepriseId}/{machineId}")
    public String updateMachine(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                @ModelAttribute("machineForm") MachineForm machineForm,
                                @RequestParam(value = "photoFiles", required = false) MultipartFile[] photoFiles,
                                @RequestParam(value = "deletePhotos", required = false) List<String> deletePhotos,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        if (!StringUtils.hasText(machineForm.getNom())) {
            redirectAttributes.addFlashAttribute("error", "Le nom de la machine est obligatoire.");
            return "redirect:/machines/" + entrepriseId + "/" + machineId + "/edit";
        }

        try {
            List<String> updatedPhotos = machineForm.getExistingPhotos() != null ? new ArrayList<>(machineForm.getExistingPhotos()) : new ArrayList<>();

            if (deletePhotos != null) {
                for (String photoUrl : deletePhotos) {
                    if (updatedPhotos.remove(photoUrl)) {
                        localFileStorageService.deletePhoto(photoUrl);
                    }
                }
            }

            int remainingSlots = Math.max(0, 3 - updatedPhotos.size());
            if (remainingSlots > 0) {
                List<String> uploadedPhotos = localFileStorageService.saveMachinePhotos(entrepriseId, machineId, photoFiles, remainingSlots);
                updatedPhotos.addAll(uploadedPhotos);
            }

            Machine machine = machineForm.toMachine();
            machine.setPhotos(updatedPhotos);

            machineService.updateMachine(entrepriseId, machineId, machine);
            redirectAttributes.addFlashAttribute("success", "Machine mise à jour avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour de la machine: " + e.getMessage());
        }

        return "redirect:/machines?entrepriseId=" + entrepriseId;
    }

    @PostMapping("/{entrepriseId}/{machineId}/delete")
    public String deleteMachine(@PathVariable String entrepriseId,
                                @PathVariable String machineId,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (!ensureSuperAdmin(request, redirectAttributes)) {
            return "redirect:/login";
        }

        try {
            Machine existing = machineService.getMachine(entrepriseId, machineId);
            if (existing != null && existing.getPhotos() != null) {
                for (String photo : existing.getPhotos()) {
                    localFileStorageService.deletePhoto(photo);
                }
            }

            machineService.deleteMachine(entrepriseId, machineId);
            redirectAttributes.addFlashAttribute("success", "Machine supprimée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression de la machine: " + e.getMessage());
        }

        return "redirect:/machines?entrepriseId=" + entrepriseId;
    }
}

