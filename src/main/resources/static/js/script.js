// État de l'application
let currentUser = null;
let currentEnterprise = null;
let enterprises = [];
let machines = [];
let categories = [];
let subCategories = [];
let maintenanceAlerts = [];
let maintenanceHistory = [];
let repairs = [];
let repairHistory = [];
let technicians = [];
let inventory = [];
let scheduledMaintenances = [];
let holidays = []; // Jours fériés
let userSchedules = []; // Planning des utilisateurs
let tickets = [];
let users = [];
let customFields = {};

// Vérifier si l'utilisateur est connecté
function checkLogin() {
    const storedUser = localStorage.getItem('currentUser');
    
    if (!storedUser) {
        window.location.href = '/login';
        return false;
    }
    
    currentUser = JSON.parse(storedUser);
    loadEnterprises();
    
    // Charger l'entreprise actuelle ou en créer une par défaut
    if (!currentEnterprise && enterprises.length === 0) {
        // Créer une entreprise par défaut
        currentEnterprise = {
            id: 1,
            name: 'Mon Entreprise',
            address: '',
            city: '',
            postalCode: '',
            phone: '',
            email: '',
            createdAt: new Date(),
            isDefault: true
        };
        enterprises.push(currentEnterprise);
        localStorage.setItem('enterprises', JSON.stringify(enterprises));
        localStorage.setItem('currentEnterprise', JSON.stringify(currentEnterprise));
    } else if (!currentEnterprise && enterprises.length > 0) {
        currentEnterprise = enterprises[0];
        localStorage.setItem('currentEnterprise', JSON.stringify(currentEnterprise));
    }
    
    // Afficher l'entreprise actuelle
    displayCurrentEnterprise();
    
    // Charger les données de l'entreprise
    loadDataForEnterprise();
    
    return true;
}

// Charger les entreprises
function loadEnterprises() {
    const stored = localStorage.getItem('enterprises');
    if (stored) {
        enterprises = JSON.parse(stored);
    } else {
        // Créer des entreprises par défaut
        enterprises = [
            {
                id: 1,
                name: 'Entreprise Exemple 1',
                address: '123 Rue Example',
                city: 'Paris',
                postalCode: '75000',
                phone: '+33 1 23 45 67 89',
                email: 'contact@exemple1.fr',
                createdAt: new Date('2023-01-01'),
                isDefault: true
            }
        ];
        localStorage.setItem('enterprises', JSON.stringify(enterprises));
    }
    
    // Si aucune entreprise n'est sélectionnée, utiliser la première
    if (!currentEnterprise && enterprises.length > 0) {
        currentEnterprise = enterprises.find(e => e.isDefault) || enterprises[0];
        localStorage.setItem('currentEnterprise', JSON.stringify(currentEnterprise));
    }
}

// Afficher l'entreprise actuelle
function displayCurrentEnterprise() {
    if (currentEnterprise) {
        document.getElementById('currentEnterpriseName').textContent = currentEnterprise.name;
        const mobileInfo = document.getElementById('currentEnterpriseMobile');
        if (mobileInfo) {
            mobileInfo.textContent = currentEnterprise.name;
        }
    }
}

// Afficher le sélecteur d'entreprise
function showEnterpriseSelector() {
    const modal = document.getElementById('selectEnterpriseModal');
    const list = document.getElementById('selectEnterpriseList');
    
    list.innerHTML = '';
    
    enterprises.forEach(enterprise => {
        const item = document.createElement('div');
        item.style.cssText = `
            padding: 1rem;
            border: 2px solid var(--border-color);
            border-radius: 8px;
            margin-bottom: 0.75rem;
            cursor: pointer;
            transition: all 0.2s ease;
            ${currentEnterprise && enterprise.id === currentEnterprise.id ? 'background: #dbeafe; border-color: var(--primary-color);' : ''}
        `;
        
        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 1rem;">
                <i class="fas fa-building" style="font-size: 1.5rem; color: var(--primary-color);"></i>
                <div style="flex: 1;">
                    <h3 style="margin: 0; color: var(--text-primary);">${enterprise.name}</h3>
                    <p style="margin: 0.25rem 0 0 0; color: var(--text-secondary); font-size: 0.85rem;">
                        ${enterprise.city} - ${enterprise.address}
                    </p>
                </div>
                ${currentEnterprise && enterprise.id === currentEnterprise.id ? 
                    '<i class="fas fa-check-circle" style="color: var(--success-color); font-size: 1.5rem;"></i>' : ''}
            </div>
        `;
        
        item.addEventListener('click', () => switchEnterprise(enterprise.id));
        list.appendChild(item);
    });
    
    modal.classList.add('show');
}

// Changer d'entreprise
function switchEnterprise(enterpriseId) {
    const enterprise = enterprises.find(e => e.id == enterpriseId);
    if (enterprise) {
        // Sauvegarder les données de l'entreprise précédente
        saveCurrentEnterpriseData();
        
        // Changer d'entreprise
        currentEnterprise = enterprise;
        localStorage.setItem('currentEnterprise', JSON.stringify(enterprise));
        displayCurrentEnterprise();
        
        // Recharger les données de la nouvelle entreprise
        loadDataForEnterprise();
        
        // Recharger toutes les données
        reloadAllData();
        
        // Fermer le modal et notifier
        closeModal('selectEnterpriseModal');
        showNotification('Entreprise changée avec succès - Les données ont été mises à jour', 'success');
        
        // Recharger la page des entreprises si elle est active
        if (document.getElementById('enterprises').classList.contains('active')) {
            renderEnterprises();
        }
    }
}

// Changer d'entreprise (fonction publique)
function changeEnterprise() {
    showEnterpriseSelector();
}

// Fonction pour recharger toutes les données après changement d'entreprise
function reloadAllData() {
    updateDashboard();
    
    // Mettre à jour toutes les pages actives
    const activePage = document.querySelector('.content-page.active');
    if (activePage) {
        const pageId = activePage.id || activePage.getAttribute('id');
        switch(pageId) {
            case 'machines':
                renderMachinesTable();
                break;
            case 'categories':
                renderCategories();
                break;
            case 'repairs':
                renderRepairs();
                break;
            case 'tickets':
                renderTickets();
                break;
            case 'inventory':
                renderInventory();
                break;
            case 'users':
                renderUsers();
                break;
            case 'reports':
                renderReports();
                break;
            case 'calendar':
                renderCalendar();
                break;
        }
    }
}

// Sauvegarder les données de l'entreprise actuelle
function saveCurrentEnterpriseData() {
    if (!currentEnterprise) return;
    
    const enterpriseData = {
        machines: machines,
        categories: categories,
        subCategories: subCategories,
        repairs: repairs,
        maintenanceAlerts: maintenanceAlerts,
        maintenanceHistory: maintenanceHistory,
        technicians: technicians,
        inventory: inventory,
        tickets: tickets,
        users: users,
        holidays: holidays,
        userSchedules: userSchedules
    };
    
    localStorage.setItem(`enterprise_data_${currentEnterprise.id}`, JSON.stringify(enterpriseData));
}

// Charger les données pour l'entreprise actuelle
function loadDataForEnterprise() {
    if (!currentEnterprise) return;
    
    console.log('Chargement des données pour:', currentEnterprise.name);
    
    // Charger les données spécifiques à l'entreprise
    const stored = localStorage.getItem(`enterprise_data_${currentEnterprise.id}`);
    
    if (stored) {
        const enterpriseData = JSON.parse(stored);
        machines = enterpriseData.machines || [];
        categories = enterpriseData.categories || [];
        subCategories = enterpriseData.subCategories || [];
        repairs = enterpriseData.repairs || [];
        maintenanceAlerts = enterpriseData.maintenanceAlerts || [];
        maintenanceHistory = enterpriseData.maintenanceHistory || [];
        technicians = enterpriseData.technicians || [];
        inventory = enterpriseData.inventory || [];
        tickets = enterpriseData.tickets || [];
        users = enterpriseData.users || [];
        holidays = enterpriseData.holidays || [];
        userSchedules = enterpriseData.userSchedules || [];
    } else {
        // Initialiser avec les données par défaut pour cette entreprise
        initializeDefaultDataForEnterprise();
    }
    
    // Sauvegarder après chargement
    saveCurrentEnterpriseData();
}

// Initialiser les données par défaut pour une entreprise
function initializeDefaultDataForEnterprise() {
    // Charger les données par défaut
    if (demoData && demoData.machines) {
        machines = [...demoData.machines];
        categories = [...demoData.categories];
        subCategories = [...demoData.subCategories];
        repairs = [...demoData.repairs];
        maintenanceAlerts = [...demoData.maintenanceAlerts];
        maintenanceHistory = [...demoData.maintenanceHistory];
        technicians = [...demoData.technicians];
        inventory = [...demoData.inventory];
        tickets = [...demoData.tickets];
        users = [...demoData.users];
        holidays = [...demoData.holidays];
        userSchedules = [...demoData.userSchedules];
    } else {
        // Si pas de demoData, créer des tableaux vides
        machines = [];
        categories = [];
        subCategories = [];
        repairs = [];
        maintenanceAlerts = [];
        maintenanceHistory = [];
        technicians = [];
        inventory = [];
        tickets = [];
        users = [];
        holidays = [];
        userSchedules = [];
    }
}

// Afficher le modal d'ajout d'entreprise
function showAddEnterpriseModal() {
    const modal = document.getElementById('addEnterpriseModal');
    modal.classList.add('show');
}

// Gérer l'ajout d'entreprise
document.addEventListener('DOMContentLoaded', function() {
    // Vérifier la connexion uniquement si on n'est pas sur la page de login
    if (window.location.pathname.includes('index.html') || window.location.pathname === '/') {
        if (!checkLogin()) {
            return;
        }
    }
    
    // Détecter l'URL et afficher la page correspondante
    const pathname = window.location.pathname;
    let pageToShow = 'dashboard'; // Par défaut
    
    if (pathname.includes('/alerts')) {
        pageToShow = 'alerts';
    } else if (pathname.includes('/machines')) {
        pageToShow = 'machines';
    } else if (pathname.includes('/categories')) {
        pageToShow = 'categories';
    } else if (pathname.includes('/repairs')) {
        pageToShow = 'repairs';
    } else if (pathname.includes('/reports')) {
        pageToShow = 'reports';
    } else if (pathname.includes('/calendar')) {
        pageToShow = 'calendar';
    } else if (pathname.includes('/inventory')) {
        pageToShow = 'inventory';
    } else if (pathname.includes('/tickets')) {
        pageToShow = 'tickets';
    } else if (pathname.includes('/users')) {
        pageToShow = 'users';
    } else if (pathname.includes('/enterprises') || pathname.includes('/ent')) {
        pageToShow = 'enterprises';
    }
    
    // Afficher la page correspondante si la fonction existe
    if (typeof navigateToPage === 'function') {
        navigateToPage(pageToShow);
    }
    
    const addEnterpriseForm = document.getElementById('addEnterpriseForm');
    if (addEnterpriseForm) {
        addEnterpriseForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const newEnterprise = {
                id: Date.now(),
                name: document.getElementById('enterpriseName').value,
                address: document.getElementById('enterpriseAddress').value,
                city: document.getElementById('enterpriseCity').value,
                postalCode: document.getElementById('enterprisePostalCode').value,
                phone: document.getElementById('enterprisePhone').value,
                email: document.getElementById('enterpriseEmail').value,
                createdAt: new Date()
            };
            
            enterprises.push(newEnterprise);
            localStorage.setItem('enterprises', JSON.stringify(enterprises));
            
            closeModal('addEnterpriseModal');
            showNotification('Entreprise ajoutée avec succès', 'success');
            addEnterpriseForm.reset();
            
            // Recharger la page des entreprises si on est sur cette page
            if (document.getElementById('enterprises') && document.getElementById('enterprises').classList.contains('active')) {
                renderEnterprises();
            }
        });
    }
});

// Données de démonstration
const demoData = {
    categories: [
        { id: 1, name: 'Machines Industrielles', description: 'Machines de production industrielle' },
        { id: 2, name: 'Équipements IT', description: 'Serveurs, ordinateurs, réseau' },
        { id: 3, name: 'Machines de Production', description: 'Machines de fabrication' }
    ],
    subCategories: [
        { id: 1, name: 'Presse Hydraulique', parentId: 1, description: 'Presses pour formage' },
        { id: 2, name: 'Serveurs', parentId: 2, description: 'Serveurs de données' },
        { id: 3, name: 'Ordinateurs', parentId: 2, description: 'PC de bureau et portables' },
        { id: 4, name: 'Convoyeur', parentId: 3, description: 'Systèmes de convoyage' }
    ],
    machines: [
        {
            id: 1,
            name: 'Presse Hydraulique #1',
            location: 'Atelier A',
            serialNumber: 'PH-001-2023',
            categoryId: 1,
            subCategoryId: 1,
            customFields: { poids: '2500 kg', puissance: '15 kW' },
            createdAt: new Date('2023-01-15')
        },
        {
            id: 2,
            name: 'Serveur Principal',
            location: 'Salle Serveur',
            serialNumber: 'SRV-001-2023',
            categoryId: 2,
            subCategoryId: 2,
            customFields: { ram: '64 GB', stockage: '2 TB SSD' },
            createdAt: new Date('2023-02-20')
        }
    ],
    repairs: [
        {
            id: 1,
            machineId: 1,
            title: 'Panne moteur',
            description: 'Le moteur de la presse hydraulique ne démarre plus. Bruit anormal au démarrage.',
            priority: 'high',
            status: 'in_progress',
            technician: 'Jean Dupont',
            estimatedCost: 850.00,
            actualCost: 0,
            estimatedDuration: 4,
            actualDuration: 0,
            createdAt: new Date('2023-12-01'),
            startedAt: new Date('2023-12-02'),
            completedAt: null,
            notes: ''
        },
        {
            id: 2,
            machineId: 2,
            title: 'Ventilateur défaillant',
            description: 'Le ventilateur du serveur fait un bruit anormal et la température monte.',
            priority: 'medium',
            status: 'completed',
            technician: 'Marie Martin',
            estimatedCost: 120.00,
            actualCost: 95.00,
            estimatedDuration: 2,
            actualDuration: 1.5,
            createdAt: new Date('2023-11-28'),
            startedAt: new Date('2023-11-28'),
            completedAt: new Date('2023-11-28'),
            notes: 'Ventilateur remplacé avec succès. Température normale.'
        }
    ],
    maintenanceAlerts: [
        {
            id: 1,
            machineId: 1,
            date: '2023-12-15',
            time: '09:00',
            description: 'Maintenance préventive mensuelle - Vérification des niveaux hydrauliques et lubrifiants',
            frequency: 'monthly',
            completed: false,
            createdAt: new Date('2023-11-15')
        },
        {
            id: 2,
            machineId: 2,
            date: '2023-12-20',
            time: '14:00',
            description: 'Maintenance trimestrielle - Nettoyage des ventilateurs et vérification des connexions',
            frequency: 'quarterly',
            completed: false,
            createdAt: new Date('2023-09-20')
        },
        {
            id: 3,
            machineId: 3,
            date: '2023-12-10',
            time: '08:30',
            description: 'Maintenance annuelle - Révision complète et remplacement des pièces d\'usure',
            frequency: 'annually',
            completed: false,
            createdAt: new Date('2022-12-10')
        },
        {
            id: 4,
            machineId: 1,
            date: '2023-12-05',
            time: '10:00',
            description: 'Maintenance urgente - Réparation du système de refroidissement',
            frequency: 'custom',
            completed: false,
            createdAt: new Date('2023-12-01')
        }
    ],
    maintenanceHistory: [
        {
            id: 1,
            machineId: 1,
            date: '2023-11-15',
            description: 'Maintenance préventive mensuelle - Vérification des niveaux hydrauliques',
            technician: 'Jean Dupont',
            duration: 2.5,
            notes: 'Maintenance effectuée avec succès. Niveaux corrects.',
            completedAt: new Date('2023-11-15')
        },
        {
            id: 2,
            machineId: 2,
            date: '2023-11-10',
            description: 'Maintenance trimestrielle - Nettoyage des ventilateurs',
            technician: 'Marie Martin',
            duration: 1.5,
            notes: 'Ventilateurs nettoyés. Température optimale.',
            completedAt: new Date('2023-11-10')
        },
        {
            id: 3,
            machineId: 3,
            date: '2023-10-25',
            description: 'Maintenance corrective - Remplacement du capteur de pression',
            technician: 'Pierre Leroy',
            duration: 3.0,
            notes: 'Capteur remplacé. Machine opérationnelle.',
            completedAt: new Date('2023-10-25')
        }
    ],
    technicians: [
        {
            id: 1,
            name: 'Jean Dupont',
            email: 'jean.dupont@maintenance.com',
            phone: '01 23 45 67 89',
            specialty: 'mechanical',
            level: 'senior',
            availability: 'full_time',
            status: 'available',
            workload: 75,
            createdAt: new Date('2023-01-15')
        },
        {
            id: 2,
            name: 'Marie Martin',
            email: 'marie.martin@maintenance.com',
            phone: '01 23 45 67 90',
            specialty: 'electrical',
            level: 'expert',
            availability: 'full_time',
            status: 'available',
            workload: 60,
            createdAt: new Date('2023-02-01')
        },
        {
            id: 3,
            name: 'Pierre Leroy',
            email: 'pierre.leroy@maintenance.com',
            phone: '01 23 45 67 91',
            specialty: 'hydraulic',
            level: 'junior',
            availability: 'part_time',
            status: 'busy',
            workload: 90,
            createdAt: new Date('2023-03-01')
        }
    ],
    inventory: [
        {
            id: 1,
            name: 'Moteur Électrique 5kW',
            partNumber: 'MOT-5KW-001',
            category: 'electrical',
            quantity: 12,
            minStock: 5,
            price: 850.00,
            supplier: 'ElectroParts',
            location: 'Rack A, Étagère 2',
            status: 'in_stock'
        },
        {
            id: 2,
            name: 'Pompe Hydraulique',
            partNumber: 'POM-HYD-002',
            category: 'hydraulic',
            quantity: 3,
            minStock: 5,
            price: 1200.00,
            supplier: 'HydraTech',
            location: 'Rack B, Étagère 1',
            status: 'low_stock'
        },
        {
            id: 3,
            name: 'Capteur de Pression',
            partNumber: 'CAP-PRESS-003',
            category: 'electrical',
            quantity: 0,
            minStock: 10,
            price: 150.00,
            supplier: 'SensorPro',
            location: 'Rack C, Étagère 3',
            status: 'out_of_stock'
        },
        {
            id: 4,
            name: 'Joint d\'Étanchéité',
            partNumber: 'JOINT-004',
            category: 'mechanical',
            quantity: 50,
            minStock: 20,
            price: 25.00,
            supplier: 'SealMaster',
            location: 'Rack D, Étagère 1',
            status: 'in_stock'
        }
    ],
    tickets: [
        {
            id: 1,
            ticketNumber: 'TKT-001',
            title: 'Panne moteur presse hydraulique',
            description: 'Le moteur de la presse hydraulique ne démarre plus. Bruit anormal au démarrage.',
            priority: 'high',
            status: 'in_progress',
            category: 'repair',
            machineId: 1,
            assigneeId: 1,
            createdBy: 1,
            createdAt: new Date('2023-12-01'),
            updatedAt: new Date('2023-12-02'),
            expectedDate: new Date('2023-12-05'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        },
        {
            id: 2,
            ticketNumber: 'TKT-002',
            title: 'Maintenance préventive serveur',
            description: 'Maintenance préventive programmée pour le serveur principal.',
            priority: 'medium',
            status: 'open',
            category: 'maintenance',
            machineId: 2,
            assigneeId: 2,
            createdBy: 1,
            createdAt: new Date('2023-12-02'),
            updatedAt: new Date('2023-12-02'),
            expectedDate: new Date('2023-12-10'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        },
        {
            id: 3,
            ticketNumber: 'TKT-003',
            title: 'Installation machine #005',
            description: 'Installation et mise en service de la nouvelle machine',
            priority: 'high',
            status: 'open',
            category: 'installation',
            machineId: 5,
            assigneeId: 1,
            createdBy: 3,
            createdAt: new Date('2024-01-15'),
            updatedAt: new Date('2024-01-15'),
            expectedDate: new Date('2024-01-25'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        },
        {
            id: 4,
            ticketNumber: 'TKT-004',
            title: 'Formation équipe nouveau système',
            description: 'Formation sur le nouveau logiciel de maintenance',
            priority: 'medium',
            status: 'open',
            category: 'training',
            machineId: null,
            assigneeId: 2,
            createdBy: 3,
            createdAt: new Date('2024-02-10'),
            updatedAt: new Date('2024-02-10'),
            expectedDate: new Date('2024-02-20'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        },
        {
            id: 5,
            ticketNumber: 'TKT-005',
            title: 'Réparation urgente machine #003',
            description: 'Panne critique nécessitant intervention immédiate',
            priority: 'urgent',
            status: 'open',
            category: 'repair',
            machineId: 3,
            assigneeId: 4,
            createdBy: 4,
            createdAt: new Date('2024-03-05'),
            updatedAt: new Date('2024-03-05'),
            expectedDate: new Date('2024-03-07'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        },
        {
            id: 6,
            ticketNumber: 'TKT-006',
            title: 'Audit sécurité équipements',
            description: 'Audit trimestriel de sécurité des équipements',
            priority: 'high',
            status: 'open',
            category: 'audit',
            machineId: null,
            assigneeId: 3,
            createdBy: 3,
            createdAt: new Date('2024-04-01'),
            updatedAt: new Date('2024-04-01'),
            expectedDate: new Date('2024-04-15'),
            resolvedAt: null,
            closedAt: null,
            comments: []
        }
    ],
    users: [
        {
            id: 1,
            name: 'Patrice Martin',
            email: 'patrice@maintenance.com',
            username: 'patrice',
            password: 'CHANGEME', // ⚠️ Ne pas utiliser en production - données de test uniquement
            role: 'technician',
            department: 'Maintenance',
            phone: '01 23 45 67 88',
            status: 'active',
            createdAt: new Date('2023-01-01'),
            workingHours: { start: '08:00', end: '17:00' },
            workingDays: [1, 2, 3, 4, 5],
            employeeType: 'full-time',
            weekendDays: [0, 6],
            customSchedule: {
                monday: { start: '08:00', end: '17:00', working: true },
                tuesday: { start: '08:00', end: '17:00', working: true },
                wednesday: { start: '08:00', end: '17:00', working: true },
                thursday: { start: '08:00', end: '17:00', working: true },
                friday: { start: '08:00', end: '17:00', working: true },
                saturday: { start: '00:00', end: '00:00', working: false },
                sunday: { start: '00:00', end: '00:00', working: false }
            }
        },
        {
            id: 2,
            name: 'David Dubois',
            email: 'david@maintenance.com',
            username: 'david',
            password: 'CHANGEME', // ⚠️ Ne pas utiliser en production - données de test uniquement
            role: 'technician',
            department: 'Maintenance',
            phone: '01 23 45 67 89',
            status: 'active',
            createdAt: new Date('2023-01-15'),
            workingHours: { start: '09:00', end: '18:00' },
            workingDays: [1, 2, 3, 4, 5],
            employeeType: 'alternant',
            weekendDays: [0, 6],
            alternantSchedule: {
                week1: [1, 2, 3],
                week2: [4, 5],
                currentWeek: 1
            },
            customSchedule: {
                monday: { start: '09:00', end: '18:00', working: true },
                tuesday: { start: '09:00', end: '18:00', working: true },
                wednesday: { start: '09:00', end: '18:00', working: true },
                thursday: { start: '00:00', end: '00:00', working: false },
                friday: { start: '00:00', end: '00:00', working: false },
                saturday: { start: '00:00', end: '00:00', working: false },
                sunday: { start: '00:00', end: '00:00', working: false }
            }
        },
        {
            id: 3,
            name: 'Sophie Leroy',
            email: 'sophie@maintenance.com',
            username: 'sophie',
            password: 'CHANGEME', // ⚠️ Ne pas utiliser en production - données de test uniquement
            role: 'manager',
            department: 'Maintenance',
            phone: '01 23 45 67 90',
            status: 'active',
            createdAt: new Date('2023-02-01'),
            workingHours: { start: '08:30', end: '17:30' },
            workingDays: [1, 2, 3, 4, 5],
            employeeType: 'full-time',
            weekendDays: [0, 6],
            customSchedule: {
                monday: { start: '08:30', end: '17:30', working: true },
                tuesday: { start: '08:30', end: '17:30', working: true },
                wednesday: { start: '08:30', end: '17:30', working: true },
                thursday: { start: '08:30', end: '17:30', working: true },
                friday: { start: '08:30', end: '17:30', working: true },
                saturday: { start: '00:00', end: '00:00', working: false },
                sunday: { start: '00:00', end: '00:00', working: false }
            }
        },
        {
            id: 4,
            name: 'Thomas Bernard',
            email: 'thomas@maintenance.com',
            username: 'thomas',
            password: 'CHANGEME', // ⚠️ Ne pas utiliser en production - données de test uniquement
            role: 'technician',
            department: 'Maintenance',
            phone: '01 23 45 67 91',
            status: 'active',
            createdAt: new Date('2023-02-15'),
            workingHours: { start: '07:00', end: '16:00' },
            workingDays: [1, 2, 3, 4, 5, 6], // Travaille aussi le samedi
            employeeType: 'full-time',
            weekendDays: [0], // Seulement dimanche
            customSchedule: {
                monday: { start: '07:00', end: '16:00', working: true },
                tuesday: { start: '07:00', end: '16:00', working: true },
                wednesday: { start: '07:00', end: '16:00', working: true },
                thursday: { start: '07:00', end: '16:00', working: true },
                friday: { start: '07:00', end: '16:00', working: true },
                saturday: { start: '08:00', end: '12:00', working: true },
                sunday: { start: '00:00', end: '00:00', working: false }
            }
        }
    ],

    // Jours fériés 2025
    holidays: [
        { id: 1, name: 'Jour de l\'An', date: '2025-01-01', type: 'holiday' },
        { id: 2, name: 'Lundi de Pâques', date: '2025-04-21', type: 'holiday' },
        { id: 3, name: 'Fête du Travail', date: '2025-05-01', type: 'holiday' },
        { id: 4, name: 'Victoire 1945', date: '2025-05-08', type: 'holiday' },
        { id: 5, name: 'Ascension', date: '2025-05-29', type: 'holiday' },
        { id: 6, name: 'Lundi de Pentecôte', date: '2025-06-09', type: 'holiday' },
        { id: 7, name: 'Fête Nationale', date: '2025-07-14', type: 'holiday' },
        { id: 8, name: 'Assomption', date: '2025-08-15', type: 'holiday' },
        { id: 9, name: 'Toussaint', date: '2025-11-01', type: 'holiday' },
        { id: 10, name: 'Armistice', date: '2025-11-11', type: 'holiday' },
        { id: 11, name: 'Noël', date: '2025-12-25', type: 'holiday' }
    ],

    // Planning statique complet pour octobre-décembre 2025
    userSchedules: [
        // === OCTOBRE 2025 ===
        // Patrice Martin (ID: 1) - Bleu
        { id: 1, userId: 1, date: '2025-10-02', type: 'training', description: 'Formation sécurité', isFullDay: true },
        { id: 2, userId: 1, date: '2025-10-08', type: 'maintenance_preventive', description: 'Maintenance machine #001', isFullDay: false, startTime: '08:00', endTime: '12:00' },
        { id: 3, userId: 1, date: '2025-10-15', type: 'vacation', description: 'Congés Toussaint', isFullDay: true },
        { id: 4, userId: 1, date: '2025-10-16', type: 'vacation', description: 'Congés Toussaint', isFullDay: true },
        { id: 5, userId: 1, date: '2025-10-22', type: 'machine_arrival', description: 'Installation machine #006', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 6, userId: 1, date: '2025-10-28', type: 'inspection', description: 'Inspection trimestrielle', isFullDay: false, startTime: '08:00', endTime: '16:00' },

        // David Dubois (ID: 2) - Vert
        { id: 7, userId: 2, date: '2025-10-03', type: 'equipment_test', description: 'Test nouveau équipement', isFullDay: false, startTime: '14:00', endTime: '18:00' },
        { id: 8, userId: 2, date: '2025-10-10', type: 'training', description: 'Formation technique', isFullDay: true },
        { id: 9, userId: 2, date: '2025-10-17', type: 'sick', description: 'Arrêt maladie', isFullDay: true },
        { id: 10, userId: 2, date: '2025-10-24', type: 'installation', description: 'Installation système', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 11, userId: 2, date: '2025-10-30', type: 'meeting', description: 'Réunion équipe', isFullDay: false, startTime: '14:00', endTime: '16:00' },

        // Sophie Leroy (ID: 3) - Violet
        { id: 12, userId: 3, date: '2025-10-01', type: 'audit', description: 'Audit Q4', isFullDay: false, startTime: '08:30', endTime: '17:30' },
        { id: 13, userId: 3, date: '2025-10-09', type: 'meeting', description: 'Réunion direction', isFullDay: false, startTime: '14:00', endTime: '18:00' },
        { id: 14, userId: 3, date: '2025-10-18', type: 'vacation', description: 'Congés personnels', isFullDay: true },
        { id: 15, userId: 3, date: '2025-10-25', type: 'training', description: 'Formation management', isFullDay: true },
        { id: 16, userId: 3, date: '2025-10-31', type: 'machine_arrival', description: 'Réception machine #007', isFullDay: false, startTime: '10:00', endTime: '18:00' },

        // Thomas Bernard (ID: 4) - Orange
        { id: 17, userId: 4, date: '2025-10-04', type: 'repair_scheduled', description: 'Réparation urgente', isFullDay: false, startTime: '07:00', endTime: '14:00' },
        { id: 18, userId: 4, date: '2025-10-11', type: 'maintenance_preventive', description: 'Maintenance mensuelle', isFullDay: false, startTime: '07:00', endTime: '12:00' },
        { id: 19, userId: 4, date: '2025-10-19', type: 'vacation', description: 'Congés week-end', isFullDay: true },
        { id: 20, userId: 4, date: '2025-10-26', type: 'inspection', description: 'Inspection équipements', isFullDay: false, startTime: '08:00', endTime: '16:00' },
        { id: 21, userId: 4, date: '2025-10-29', type: 'equipment_test', description: 'Test performance', isFullDay: false, startTime: '09:00', endTime: '15:00' },

        // === NOVEMBRE 2025 ===
        // Patrice Martin
        { id: 22, userId: 1, date: '2025-11-05', type: 'audit', description: 'Audit sécurité', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 23, userId: 1, date: '2025-11-12', type: 'training', description: 'Formation hiver', isFullDay: true },
        { id: 24, userId: 1, date: '2025-11-19', type: 'maintenance_preventive', description: 'Maintenance trimestrielle', isFullDay: false, startTime: '08:00', endTime: '15:00' },
        { id: 25, userId: 1, date: '2025-11-26', type: 'vacation', description: 'Congés novembre', isFullDay: true },

        // David Dubois
        { id: 26, userId: 2, date: '2025-11-06', type: 'installation', description: 'Installation automne', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 27, userId: 2, date: '2025-11-13', type: 'equipment_test', description: 'Test pré-hiver', isFullDay: false, startTime: '14:00', endTime: '18:00' },
        { id: 28, userId: 2, date: '2025-11-20', type: 'sick', description: 'Arrêt maladie', isFullDay: true },
        { id: 29, userId: 2, date: '2025-11-27', type: 'meeting', description: 'Réunion planning', isFullDay: false, startTime: '14:00', endTime: '17:00' },

        // Sophie Leroy
        { id: 30, userId: 3, date: '2025-11-07', type: 'audit', description: 'Audit qualité', isFullDay: false, startTime: '08:30', endTime: '17:30' },
        { id: 31, userId: 3, date: '2025-11-14', type: 'training', description: 'Formation équipe', isFullDay: true },
        { id: 32, userId: 3, date: '2025-11-21', type: 'meeting', description: 'Réunion bilan', isFullDay: false, startTime: '15:00', endTime: '18:00' },
        { id: 33, userId: 3, date: '2025-11-28', type: 'vacation', description: 'Congés novembre', isFullDay: true },

        // Thomas Bernard
        { id: 34, userId: 4, date: '2025-11-08', type: 'repair_scheduled', description: 'Réparation programmée', isFullDay: false, startTime: '07:00', endTime: '16:00' },
        { id: 35, userId: 4, date: '2025-11-15', type: 'maintenance_preventive', description: 'Maintenance novembre', isFullDay: false, startTime: '07:00', endTime: '13:00' },
        { id: 36, userId: 4, date: '2025-11-22', type: 'inspection', description: 'Inspection novembre', isFullDay: false, startTime: '08:00', endTime: '16:00' },
        { id: 37, userId: 4, date: '2025-11-29', type: 'equipment_test', description: 'Test fin mois', isFullDay: false, startTime: '09:00', endTime: '15:00' },

        // === DÉCEMBRE 2025 ===
        // Patrice Martin
        { id: 38, userId: 1, date: '2025-12-03', type: 'machine_arrival', description: 'Installation machine #008', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 39, userId: 1, date: '2025-12-10', type: 'training', description: 'Formation fin année', isFullDay: true },
        { id: 40, userId: 1, date: '2025-12-17', type: 'audit', description: 'Audit annuel', isFullDay: false, startTime: '09:00', endTime: '18:00' },
        { id: 41, userId: 1, date: '2025-12-23', type: 'vacation', description: 'Congés Noël', isFullDay: true },
        { id: 42, userId: 1, date: '2025-12-24', type: 'vacation', description: 'Congés Noël', isFullDay: true },
        { id: 43, userId: 1, date: '2025-12-30', type: 'maintenance_preventive', description: 'Maintenance fin année', isFullDay: false, startTime: '08:30', endTime: '12:30' },

        // David Dubois
        { id: 44, userId: 2, date: '2025-12-04', type: 'installation', description: 'Installation fin année', isFullDay: false, startTime: '09:00', endTime: '17:00' },
        { id: 45, userId: 2, date: '2025-12-11', type: 'equipment_test', description: 'Test annuel', isFullDay: false, startTime: '14:00', endTime: '18:00' },
        { id: 46, userId: 2, date: '2025-12-18', type: 'meeting', description: 'Réunion fin année', isFullDay: false, startTime: '14:00', endTime: '17:00' },
        { id: 47, userId: 2, date: '2025-12-31', type: 'vacation', description: 'Congés réveillon', isFullDay: true },

        // Sophie Leroy
        { id: 48, userId: 3, date: '2025-12-05', type: 'audit', description: 'Audit fin année', isFullDay: false, startTime: '08:30', endTime: '17:30' },
        { id: 49, userId: 3, date: '2025-12-12', type: 'training', description: 'Formation bilan', isFullDay: true },
        { id: 50, userId: 3, date: '2025-12-19', type: 'meeting', description: 'Réunion direction', isFullDay: false, startTime: '15:00', endTime: '18:00' },
        { id: 51, userId: 3, date: '2025-12-26', type: 'vacation', description: 'Congés post-Noël', isFullDay: true },
        { id: 52, userId: 3, date: '2025-12-27', type: 'vacation', description: 'Congés post-Noël', isFullDay: true },

        // Thomas Bernard
        { id: 53, userId: 4, date: '2025-12-06', type: 'repair_scheduled', description: 'Réparation urgente', isFullDay: false, startTime: '07:00', endTime: '16:00' },
        { id: 54, userId: 4, date: '2025-12-13', type: 'maintenance_preventive', description: 'Maintenance décembre', isFullDay: false, startTime: '07:00', endTime: '13:00' },
        { id: 55, userId: 4, date: '2025-12-20', type: 'inspection', description: 'Inspection finale', isFullDay: false, startTime: '08:00', endTime: '16:00' },
        { id: 56, userId: 4, date: '2025-12-28', type: 'equipment_test', description: 'Test final', isFullDay: false, startTime: '09:00', endTime: '15:00' }
    ]
};

// Initialisation de l'application
document.addEventListener('DOMContentLoaded', function() {
    // Définir l'utilisateur par défaut
    currentUser = {
        id: 0,
        name: 'Administrateur',
        username: 'admin',
        role: 'admin'
    };
    
    
    // Vérifier si on est sur la page principale (index.html)
    const isMainPage = document.getElementById('machinesTableBody') !== null;
    
    if (isMainPage) {
    initializeApp();
    setupEventListeners();
    loadDemoData();
    updateDashboard();
    
    // Initialiser la navigation sur le dashboard
    setTimeout(() => {
        navigateToPage('dashboard');
    }, 100);
    
    // Vérifier les notifications de tickets au chargement
    checkTicketNotifications();
    
    // Vérifier les notifications toutes les 30 secondes
    setInterval(checkTicketNotifications, 30000);
    } else {
        // Sur les pages séparées (users.html, enterprises.html), juste initialiser les événements de base
        setupBasicEventListeners();
    }
    
});

function initializeApp() {
    // Ne s'exécuter que si on est sur index.html (dashboard)
    if (!document.getElementById('machinesTableBody')) {
        return; // Pas sur la page principale, ne rien faire
    }
    // Charger les données depuis le localStorage ou utiliser les données de démo
    loadDataFromStorage();
    renderMachinesTable();
    renderCategories();
    updateCategorySelects();
}

function setupBasicEventListeners() {
    // Événements de base pour toutes les pages
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
}

function setupEventListeners() {
    setupBasicEventListeners();
    // Connexion
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
    
    // Navigation
    const menuItems = document.querySelectorAll('.menu-item');
    
    menuItems.forEach((item, index) => {
        item.addEventListener('click', function() {
            const page = this.dataset.page;
            navigateToPage(page);
        });
    });
    
    // Modals - Machines
    const addMachineForm = document.getElementById('addMachineForm');
    if (addMachineForm) addMachineForm.addEventListener('submit', handleAddMachine);
    const machineCategory = document.getElementById('machineCategory');
    if (machineCategory) machineCategory.addEventListener('change', updateSubCategorySelect);
    
    // Modals - Catégories
    const addCategoryForm = document.getElementById('addCategoryForm');
    if (addCategoryForm) addCategoryForm.addEventListener('submit', handleAddCategory);
    const addSubCategoryForm = document.getElementById('addSubCategoryForm');
    if (addSubCategoryForm) addSubCategoryForm.addEventListener('submit', handleAddSubCategory);
    
    // Modals - Réparations
    const addRepairForm = document.getElementById('addRepairForm');
    if (addRepairForm) addRepairForm.addEventListener('submit', handleAddRepair);
    
    // Modals - Techniciens
    const addTechnicianForm = document.getElementById('addTechnicianForm');
    if (addTechnicianForm) addTechnicianForm.addEventListener('submit', handleAddTechnician);
    
    // Modals - Stock
    const addInventoryForm = document.getElementById('addInventoryForm');
    if (addInventoryForm) addInventoryForm.addEventListener('submit', handleAddInventory);
    
    // Modals - Calendrier
    const addMaintenanceForm = document.getElementById('addMaintenanceForm');
    if (addMaintenanceForm) addMaintenanceForm.addEventListener('submit', handleAddMaintenance);
    
    // Modals - Tickets
    const addTicketForm = document.getElementById('addTicketForm');
    if (addTicketForm) addTicketForm.addEventListener('submit', handleAddTicket);
    
    // Modals - Utilisateurs
    const addUserForm = document.getElementById('addUserForm');
    if (addUserForm) addUserForm.addEventListener('submit', handleAddUser);
    
    // Recherche et filtres
    const machineSearch = document.getElementById('machineSearch');
    if (machineSearch) machineSearch.addEventListener('input', filterMachines);
    const categoryFilter = document.getElementById('categoryFilter');
    if (categoryFilter) categoryFilter.addEventListener('change', filterMachines);
    const repairSearch = document.getElementById('repairSearch');
    if (repairSearch) repairSearch.addEventListener('input', filterRepairs);
    const repairStatusFilter = document.getElementById('repairStatusFilter');
    if (repairStatusFilter) repairStatusFilter.addEventListener('change', filterRepairs);
    const repairPriorityFilter = document.getElementById('repairPriorityFilter');
    if (repairPriorityFilter) repairPriorityFilter.addEventListener('change', filterRepairs);
    const inventorySearch = document.getElementById('inventorySearch');
    if (inventorySearch) inventorySearch.addEventListener('input', filterInventory);
    const inventoryCategory = document.getElementById('inventoryCategory');
    if (inventoryCategory) inventoryCategory.addEventListener('change', filterInventory);
    const inventoryStatus = document.getElementById('inventoryStatus');
    if (inventoryStatus) inventoryStatus.addEventListener('change', filterInventory);
    const ticketSearch = document.getElementById('ticketSearch');
    if (ticketSearch) ticketSearch.addEventListener('input', filterTickets);
    const ticketStatusFilter = document.getElementById('ticketStatusFilter');
    if (ticketStatusFilter) ticketStatusFilter.addEventListener('change', filterTickets);
    const ticketPriorityFilter = document.getElementById('ticketPriorityFilter');
    if (ticketPriorityFilter) ticketPriorityFilter.addEventListener('change', filterTickets);
    const ticketAssigneeFilter = document.getElementById('ticketAssigneeFilter');
    if (ticketAssigneeFilter) ticketAssigneeFilter.addEventListener('change', filterTickets);
    const userSearch = document.getElementById('userSearch');
    if (userSearch) userSearch.addEventListener('input', filterUsers);
    const userRoleFilter = document.getElementById('userRoleFilter');
    if (userRoleFilter) userRoleFilter.addEventListener('change', filterUsers);
    const userStatusFilter = document.getElementById('userStatusFilter');
    if (userStatusFilter) userStatusFilter.addEventListener('change', filterUsers);
    
    // Calendrier
    const calendarView = document.getElementById('calendarView');
    if (calendarView) calendarView.addEventListener('change', renderCalendar);
    const calendarMachine = document.getElementById('calendarMachine');
    if (calendarMachine) calendarMachine.addEventListener('change', renderCalendar);
    
    // Fermeture des modals
    document.querySelectorAll('.close').forEach(closeBtn => {
        closeBtn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            closeModal(modal.id);
        });
    });
    
    // Fermeture des modals en cliquant à l'extérieur
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                closeModal(this.id);
            }
        });
    });
}

function loadDemoData() {
    categories = [...demoData.categories];
    subCategories = [...demoData.subCategories];
    machines = [...demoData.machines];
    repairs = [...demoData.repairs];
    technicians = [...demoData.technicians];
    inventory = [...demoData.inventory];
    tickets = [...demoData.tickets];
    users = [...demoData.users];
    holidays = [...demoData.holidays];
    userSchedules = [...demoData.userSchedules];
    maintenanceAlerts = [...demoData.maintenanceAlerts];
    maintenanceHistory = [...demoData.maintenanceHistory];
    saveDataToStorage();
}

function loadDataFromStorage() {
    const storedCategories = localStorage.getItem('maintenance_categories');
    const storedSubCategories = localStorage.getItem('maintenance_subcategories');
    const storedMachines = localStorage.getItem('maintenance_machines');
    const storedAlerts = localStorage.getItem('maintenance_alerts');
    const storedHistory = localStorage.getItem('maintenance_history');
    const storedRepairs = localStorage.getItem('maintenance_repairs');
    const storedRepairHistory = localStorage.getItem('maintenance_repair_history');
    const storedTechnicians = localStorage.getItem('maintenance_technicians');
    const storedInventory = localStorage.getItem('maintenance_inventory');
    const storedScheduledMaintenances = localStorage.getItem('maintenance_scheduled_maintenances');
    const storedTickets = localStorage.getItem('maintenance_tickets');
    const storedUsers = localStorage.getItem('maintenance_users');
    const storedHolidays = localStorage.getItem('maintenance_holidays');
    const storedUserSchedules = localStorage.getItem('maintenance_user_schedules');
    
    if (storedCategories) categories = JSON.parse(storedCategories);
    if (storedSubCategories) subCategories = JSON.parse(storedSubCategories);
    if (storedMachines) machines = JSON.parse(storedMachines);
    if (storedAlerts) maintenanceAlerts = JSON.parse(storedAlerts);
    if (storedHistory) maintenanceHistory = JSON.parse(storedHistory);
    if (storedRepairs) repairs = JSON.parse(storedRepairs);
    if (storedRepairHistory) repairHistory = JSON.parse(storedRepairHistory);
    if (storedTechnicians) technicians = JSON.parse(storedTechnicians);
    if (storedInventory) inventory = JSON.parse(storedInventory);
    if (storedScheduledMaintenances) scheduledMaintenances = JSON.parse(storedScheduledMaintenances);
    if (storedTickets) tickets = JSON.parse(storedTickets);
    if (storedUsers) users = JSON.parse(storedUsers);
    if (storedHolidays) holidays = JSON.parse(storedHolidays);
    if (storedUserSchedules) userSchedules = JSON.parse(storedUserSchedules);
}

function saveDataToStorage() {
    // Sauvegarder dans le contexte de l'entreprise actuelle
    if (currentEnterprise) {
        saveCurrentEnterpriseData();
    } else {
        // Fallback pour la rétrocompatibilité
        localStorage.setItem('maintenance_categories', JSON.stringify(categories));
        localStorage.setItem('maintenance_subcategories', JSON.stringify(subCategories));
        localStorage.setItem('maintenance_machines', JSON.stringify(machines));
        localStorage.setItem('maintenance_alerts', JSON.stringify(maintenanceAlerts));
        localStorage.setItem('maintenance_history', JSON.stringify(maintenanceHistory));
        localStorage.setItem('maintenance_repairs', JSON.stringify(repairs));
        localStorage.setItem('maintenance_repair_history', JSON.stringify(repairHistory));
        localStorage.setItem('maintenance_technicians', JSON.stringify(technicians));
        localStorage.setItem('maintenance_inventory', JSON.stringify(inventory));
        localStorage.setItem('maintenance_scheduled_maintenances', JSON.stringify(scheduledMaintenances));
        localStorage.setItem('maintenance_tickets', JSON.stringify(tickets));
        localStorage.setItem('maintenance_users', JSON.stringify(users));
        localStorage.setItem('maintenance_holidays', JSON.stringify(holidays));
        localStorage.setItem('maintenance_user_schedules', JSON.stringify(userSchedules));
    }
}

// Authentification - handleLogout sera défini plus tard après la création des modals

// Système de notifications
function showNotification(title, message, type = 'info', duration = 5000) {
    const container = document.getElementById('notificationContainer');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    
    notification.innerHTML = `
        <div class="notification-header">
            <span class="notification-title">${title}</span>
            <button class="notification-close" onclick="removeNotification(this)">&times;</button>
        </div>
        <div class="notification-message">${message}</div>
    `;
    
    container.appendChild(notification);
    
    // Afficher la notification avec animation
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    // Supprimer automatiquement après la durée spécifiée
    setTimeout(() => {
        removeNotification(notification.querySelector('.notification-close'));
    }, duration);
    
    return notification;
}

function removeNotification(button) {
    const notification = button.closest('.notification');
    notification.classList.remove('show');
    setTimeout(() => {
        if (notification.parentNode) {
            notification.parentNode.removeChild(notification);
        }
    }, 300);
}

// Fonctions pour les notifications de tickets
function checkTicketNotifications() {
    // Vérifier que tickets est défini et est un tableau
    if (!tickets || !Array.isArray(tickets)) {
        return;
    }
    
    const urgentTickets = tickets.filter(ticket => 
        ticket && ticket.priority === 'urgent' && (ticket.status === 'open' || ticket.status === 'in_progress')
    );
    
    const overdueTickets = tickets.filter(ticket => {
        if (!ticket || !ticket.expectedDate) return false;
        const expectedDate = new Date(ticket.expectedDate);
        const now = new Date();
        return expectedDate < now && (ticket.status === 'open' || ticket.status === 'in_progress');
    });
    
    const newTickets = tickets.filter(ticket => {
        if (!ticket || !ticket.createdAt) return false;
        const createdDate = new Date(ticket.createdAt);
        const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
        return createdDate > oneHourAgo;
    });
    
    // Notifications pour tickets urgents
    if (urgentTickets.length > 0 && typeof showNotification === 'function') {
        showNotification(
            'Tickets Urgents',
            `${urgentTickets.length} ticket(s) urgent(s) nécessitent une attention immédiate`,
            'error',
            8000
        );
    }
    
    // Notifications pour tickets en retard
    if (overdueTickets.length > 0 && typeof showNotification === 'function') {
        showNotification(
            'Tickets en Retard',
            `${overdueTickets.length} ticket(s) dépassent leur date d'échéance`,
            'warning',
            7000
        );
    }
    
    // Notifications pour nouveaux tickets
    if (newTickets.length > 0 && typeof showNotification === 'function') {
        showNotification(
            'Nouveaux Tickets',
            `${newTickets.length} nouveau(x) ticket(s) créé(s) récemment`,
            'info',
            5000
        );
    }
    
    // Mettre à jour les badges de notification
    updateNotificationBadges();
}

function updateNotificationBadges() {
    const urgentTickets = tickets.filter(ticket => 
        ticket.priority === 'urgent' && (ticket.status === 'open' || ticket.status === 'in_progress')
    ).length;
    
    const newTickets = tickets.filter(ticket => {
        const createdDate = new Date(ticket.createdAt);
        const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
        return createdDate > oneDayAgo;
    }).length;
    
    // Mettre à jour le badge du menu Tickets
    const ticketsMenuItem = document.querySelector('[data-page="tickets"]');
    if (!ticketsMenuItem) {
        // L'élément n'existe pas dans le DOM (peut-être sur une autre page)
        return;
    }
    
    if (urgentTickets > 0 || newTickets > 0) {
        ticketsMenuItem.classList.add('has-notifications');
        const badge = ticketsMenuItem.querySelector('.notification-badge') || document.createElement('span');
        badge.className = 'notification-badge';
        badge.textContent = urgentTickets + newTickets;
        if (!ticketsMenuItem.querySelector('.notification-badge')) {
            ticketsMenuItem.appendChild(badge);
        }
    } else {
        ticketsMenuItem.classList.remove('has-notifications');
        const badge = ticketsMenuItem.querySelector('.notification-badge');
        if (badge) {
            badge.remove();
        }
    }
}

function sendTicketNotification(ticket, action) {
    let title, message, type;
    
    switch (action) {
        case 'created':
            title = 'Nouveau Ticket Créé';
            message = `Ticket ${ticket.ticketNumber}: ${ticket.title}`;
            type = 'info';
            break;
        case 'assigned':
            title = 'Ticket Assigné';
            message = `Ticket ${ticket.ticketNumber} assigné à ${getUserById(ticket.assigneeId)?.name || 'Un technicien'}`;
            type = 'success';
            break;
        case 'updated':
            title = 'Ticket Mis à Jour';
            message = `Ticket ${ticket.ticketNumber}: ${ticket.title}`;
            type = 'info';
            break;
        case 'resolved':
            title = 'Ticket Résolu';
            message = `Ticket ${ticket.ticketNumber} a été résolu`;
            type = 'success';
            break;
        case 'urgent':
            title = 'Ticket Urgent';
            message = `Ticket ${ticket.ticketNumber} marqué comme urgent`;
            type = 'error';
            break;
        default:
            title = 'Ticket';
            message = `Ticket ${ticket.ticketNumber}`;
            type = 'info';
    }
    
    showNotification(title, message, type, 6000);
}

function getUserById(userId) {
    return users.find(user => user.id === userId);
}

// Navigation
function navigateToPage(pageName) {
    // Mettre à jour le menu actif
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
    });
    
    const activeMenuItem = document.querySelector(`[data-page="${pageName}"]`);
    if (activeMenuItem) {
        activeMenuItem.classList.add('active');
    }
    
    // Afficher la page correspondante
    const allPages = document.querySelectorAll('.content-page');
    
    allPages.forEach(page => {
        page.style.display = 'none';
        page.style.visibility = 'hidden';
    });
    
    const targetPage = document.getElementById(pageName);
    if (targetPage) {
        targetPage.style.display = 'block';
        targetPage.style.visibility = 'visible';
    }
    
    // Mettre à jour les données selon la page
    switch(pageName) {
        case 'dashboard':
            updateDashboard();
            break;
        case 'machines':
            renderMachinesTable();
            break;
        case 'categories':
            renderCategories();
            break;
        case 'repairs':
            renderRepairs();
            break;
        case 'reports':
            renderReports();
            break;
        case 'calendar':
            renderCalendar();
            break;
        case 'inventory':
            renderInventory();
            break;
        case 'tickets':
            renderTickets();
            break;
        case 'users':
            loadUsersFromFirebase();
            break;
        case 'enterprises':
            renderEnterprises();
            break;
    }
}

// Dashboard
function updateDashboard() {
    const dashboard = document.getElementById('dashboard');
    if (!dashboard) return; // Pas sur la page dashboard
    
    // Récupérer l'entreprise sélectionnée depuis le sélecteur du dashboard
    const entrepriseSelect = document.getElementById('dashboardEntrepriseSelect');
    const entrepriseId = entrepriseSelect ? entrepriseSelect.value : null;
    
    if (!entrepriseId) {
        // Utiliser les valeurs par défaut si pas d'entreprise sélectionnée
        document.getElementById('totalMachines').textContent = '0';
        document.getElementById('openTickets').textContent = '0';
        document.getElementById('pendingMaintenance').textContent = '0';
        document.getElementById('activeRepairs').textContent = '0';
        document.getElementById('completedMaintenance').textContent = '0';
        document.getElementById('urgentTickets').textContent = '0';
        return;
    }
    
    // Charger les statistiques depuis l'API
    fetch('/dashboard/api/stats?entrepriseId=' + encodeURIComponent(entrepriseId))
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                console.error('Erreur lors du chargement des statistiques:', data.error);
                return;
            }
            
            document.getElementById('totalMachines').textContent = data.totalMachines || 0;
            document.getElementById('openTickets').textContent = data.openTickets || 0;
            document.getElementById('pendingMaintenance').textContent = data.pendingMaintenance || 0;
            document.getElementById('activeRepairs').textContent = data.activeRepairs || 0;
            document.getElementById('completedMaintenance').textContent = data.completedMaintenance || 0;
            document.getElementById('urgentTickets').textContent = data.urgentTickets || 0;
            
            // Mettre à jour le badge de notification mobile
            updateNotificationBadge();
        })
        .catch(error => {
            console.error('Erreur lors du chargement des statistiques:', error);
            // Utiliser les valeurs par défaut en cas d'erreur
            document.getElementById('totalMachines').textContent = '0';
            document.getElementById('openTickets').textContent = '0';
            document.getElementById('pendingMaintenance').textContent = '0';
            document.getElementById('activeRepairs').textContent = '0';
            document.getElementById('completedMaintenance').textContent = '0';
            document.getElementById('urgentTickets').textContent = '0';
        });
}

// Fonction pour charger les stats du dashboard quand l'entreprise change
function loadDashboardStats() {
    const entrepriseSelect = document.getElementById('dashboardEntrepriseSelect');
    const entrepriseId = entrepriseSelect ? entrepriseSelect.value : null;
    
    if (entrepriseId) {
        // Rediriger vers la page dashboard avec le paramètre entrepriseId
        window.location.href = '/dashboard?entrepriseId=' + encodeURIComponent(entrepriseId);
    } else {
        // Si aucune entreprise sélectionnée, juste mettre à jour les stats à 0
        updateDashboard();
    }
}


// Gestion des machines
function renderMachinesTable() {
    const tbody = document.getElementById('machinesTableBody');
    if (!tbody) return; // L'élément n'existe pas sur cette page
    tbody.innerHTML = machines.map(machine => {
        const category = categories.find(c => c.id === machine.categoryId);
        const subCategory = subCategories.find(sc => sc.id === machine.subCategoryId);
        
        return `
            <tr>
                <td>${machine.name}</td>
                <td>${machine.location}</td>
                <td>${machine.serialNumber}</td>
                <td>${category ? category.name : 'N/A'}</td>
                <td>${subCategory ? subCategory.name : 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editMachine(${machine.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteMachine(${machine.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterMachines() {
    const searchTerm = document.getElementById('machineSearch').value.toLowerCase();
    const categoryFilter = document.getElementById('categoryFilter').value;
    
    const filteredMachines = machines.filter(machine => {
        const matchesSearch = machine.name.toLowerCase().includes(searchTerm) ||
                            machine.location.toLowerCase().includes(searchTerm) ||
                            machine.serialNumber.toLowerCase().includes(searchTerm);
        const matchesCategory = !categoryFilter || machine.categoryId == categoryFilter;
        
        return matchesSearch && matchesCategory;
    });
    
    const tbody = document.getElementById('machinesTableBody');
    tbody.innerHTML = filteredMachines.map(machine => {
        const category = categories.find(c => c.id === machine.categoryId);
        const subCategory = subCategories.find(sc => sc.id === machine.subCategoryId);
        
        return `
            <tr>
                <td>${machine.name}</td>
                <td>${machine.location}</td>
                <td>${machine.serialNumber}</td>
                <td>${category ? category.name : 'N/A'}</td>
                <td>${subCategory ? subCategory.name : 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editMachine(${machine.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteMachine(${machine.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function showAddMachineModal() {
    updateCategorySelects();
    document.getElementById('addMachineModal').classList.add('show');
}

function handleAddMachine(e) {
    e.preventDefault();
    
    const machine = {
        id: Date.now(),
        name: document.getElementById('machineName').value,
        location: document.getElementById('machineLocation').value,
        serialNumber: document.getElementById('machineSerial').value,
        categoryId: parseInt(document.getElementById('machineCategory').value),
        subCategoryId: parseInt(document.getElementById('machineSubCategory').value) || null,
        customFields: getCustomFields(),
        createdAt: new Date()
    };
    
    machines.push(machine);
    saveDataToStorage();
    renderMachinesTable();
    updateDashboard();
    closeModal('addMachineModal');
    document.getElementById('addMachineForm').reset();
    
    showNotification('Machine ajoutée avec succès', 'success');
}

function addCustomField() {
    const customFieldsContainer = document.getElementById('customFields');
    const fieldName = prompt('Nom du champ personnalisé:');
    
    if (fieldName) {
        const fieldDiv = document.createElement('div');
        fieldDiv.className = 'custom-field';
        fieldDiv.innerHTML = `
            <div class="form-group" style="flex: 1; margin-bottom: 0;">
                <label>${fieldName}</label>
                <input type="text" name="custom_${fieldName}" placeholder="Valeur pour ${fieldName}">
            </div>
            <button type="button" onclick="removeCustomField(this)" class="btn btn-danger">
                <i class="fas fa-trash"></i>
            </button>
        `;
        customFieldsContainer.appendChild(fieldDiv);
    }
}

function removeCustomField(button) {
    button.closest('.custom-field').remove();
}

function getCustomFields() {
    const fields = {};
    document.querySelectorAll('#customFields input[name^="custom_"]').forEach(input => {
        const fieldName = input.name.replace('custom_', '');
        fields[fieldName] = input.value;
    });
    return fields;
}

function editMachine(id) {
    const machine = machines.find(m => m.id === id);
    if (!machine) return;
    
    // Remplir le formulaire avec les données existantes
    document.getElementById('machineName').value = machine.name;
    document.getElementById('machineLocation').value = machine.location;
    document.getElementById('machineSerial').value = machine.serialNumber;
    document.getElementById('machineCategory').value = machine.categoryId;
    
    // Mettre à jour les sous-catégories
    updateSubCategorySelect();
    document.getElementById('machineSubCategory').value = machine.subCategoryId || '';
    
    // Remplir les champs personnalisés
    const customFieldsContainer = document.getElementById('customFields');
    customFieldsContainer.innerHTML = '';
    Object.entries(machine.customFields || {}).forEach(([key, value]) => {
        const fieldDiv = document.createElement('div');
        fieldDiv.className = 'custom-field';
        fieldDiv.innerHTML = `
            <div class="form-group" style="flex: 1; margin-bottom: 0;">
                <label>${key}</label>
                <input type="text" name="custom_${key}" value="${value}">
            </div>
            <button type="button" onclick="removeCustomField(this)" class="btn btn-danger">
                <i class="fas fa-trash"></i>
            </button>
        `;
        customFieldsContainer.appendChild(fieldDiv);
    });
    
    // Modifier le titre du modal
    document.querySelector('#addMachineModal .modal-header h2').textContent = 'Modifier la Machine';
    
    // Changer l'action du formulaire
    const form = document.getElementById('addMachineForm');
    form.onsubmit = function(e) {
        e.preventDefault();
        updateMachine(id);
    };
    
    showAddMachineModal();
}

function updateMachine(id) {
    const machineIndex = machines.findIndex(m => m.id === id);
    if (machineIndex === -1) return;
    
    machines[machineIndex] = {
        ...machines[machineIndex],
        name: document.getElementById('machineName').value,
        location: document.getElementById('machineLocation').value,
        serialNumber: document.getElementById('machineSerial').value,
        categoryId: parseInt(document.getElementById('machineCategory').value),
        subCategoryId: parseInt(document.getElementById('machineSubCategory').value) || null,
        customFields: getCustomFields()
    };
    
    saveDataToStorage();
    renderMachinesTable();
    updateDashboard();
    closeModal('addMachineModal');
    document.getElementById('addMachineForm').reset();
    
    showNotification('Machine mise à jour avec succès', 'success');
}

function deleteMachine(id) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette machine ?')) {
        machines = machines.filter(m => m.id !== id);
        saveDataToStorage();
        renderMachinesTable();
        updateDashboard();
        showNotification('Machine supprimée avec succès', 'success');
    }
}

// Gestion des catégories
function renderCategories() {
    const categoriesList = document.getElementById('categoriesList');
    const subCategoriesList = document.getElementById('subCategoriesList');
    
    if (!categoriesList || !subCategoriesList) return; // Les éléments n'existent pas
    
    categoriesList.innerHTML = categories.map(category => `
        <div class="category-item">
            <div>
                <h3>${category.name}</h3>
                <p>${category.description || 'Aucune description'}</p>
            </div>
            <div class="item-actions">
                <button class="btn btn-sm btn-primary" onclick="editCategory(${category.id})">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteCategory(${category.id})">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
    
    subCategoriesList.innerHTML = subCategories.map(subCategory => {
        const parentCategory = categories.find(c => c.id === subCategory.parentId);
        return `
            <div class="subcategory-item">
                <div>
                    <h3>${subCategory.name}</h3>
                    <p>${parentCategory ? `Sous-catégorie de ${parentCategory.name}` : 'Catégorie parent introuvable'}</p>
                </div>
                <div class="item-actions">
                    <button class="btn btn-sm btn-primary" onclick="editSubCategory(${subCategory.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteSubCategory(${subCategory.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function updateCategorySelects() {
    const categorySelect = document.getElementById('machineCategory');
    const categoryFilter = document.getElementById('categoryFilter');
    const subCategoryParent = document.getElementById('subCategoryParent');
    
    if (!categorySelect && !categoryFilter && !subCategoryParent) return; // Les éléments n'existent pas
    
    // Mettre à jour les sélecteurs de catégorie
    [categorySelect, categoryFilter, subCategoryParent].forEach(select => {
        const currentValue = select.value;
        select.innerHTML = select === categoryFilter ? 
            '<option value="">Toutes les catégories</option>' : 
            '<option value="">Sélectionner une catégorie</option>';
        
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = category.name;
            select.appendChild(option);
        });
        
        select.value = currentValue;
    });
}

function updateSubCategorySelect() {
    const categoryId = document.getElementById('machineCategory').value;
    const subCategorySelect = document.getElementById('machineSubCategory');
    
    subCategorySelect.innerHTML = '<option value="">Sélectionner une sous-catégorie</option>';
    
    if (categoryId) {
        const categorySubCategories = subCategories.filter(sc => sc.parentId == categoryId);
        categorySubCategories.forEach(subCategory => {
            const option = document.createElement('option');
            option.value = subCategory.id;
            option.textContent = subCategory.name;
            subCategorySelect.appendChild(option);
        });
    }
}

function showAddCategoryModal() {
    document.getElementById('addCategoryModal').classList.add('show');
}

function handleAddCategory(e) {
    e.preventDefault();
    
    const category = {
        id: Date.now(),
        name: document.getElementById('categoryName').value,
        description: document.getElementById('categoryDescription').value
    };
    
    categories.push(category);
    saveDataToStorage();
    renderCategories();
    updateCategorySelects();
    closeModal('addCategoryModal');
    document.getElementById('addCategoryForm').reset();
    
    showNotification('Catégorie ajoutée avec succès', 'success');
}

function showAddSubCategoryModal() {
    updateCategorySelects();
    document.getElementById('addSubCategoryModal').classList.add('show');
}

function handleAddSubCategory(e) {
    e.preventDefault();
    
    const subCategory = {
        id: Date.now(),
        name: document.getElementById('subCategoryName').value,
        parentId: parseInt(document.getElementById('subCategoryParent').value),
        description: document.getElementById('subCategoryDescription').value
    };
    
    subCategories.push(subCategory);
    saveDataToStorage();
    renderCategories();
    updateCategorySelects();
    closeModal('addSubCategoryModal');
    document.getElementById('addSubCategoryForm').reset();
    
    showNotification('Sous-catégorie ajoutée avec succès', 'success');
}

function editCategory(id) {
    const category = categories.find(c => c.id === id);
    if (!category) return;
    
    document.getElementById('categoryName').value = category.name;
    document.getElementById('categoryDescription').value = category.description || '';
    
    const form = document.getElementById('addCategoryForm');
    form.onsubmit = function(e) {
        e.preventDefault();
        updateCategory(id);
    };
    
    document.querySelector('#addCategoryModal .modal-header h2').textContent = 'Modifier la Catégorie';
    showAddCategoryModal();
}

function updateCategory(id) {
    const categoryIndex = categories.findIndex(c => c.id === id);
    if (categoryIndex === -1) return;
    
    categories[categoryIndex] = {
        ...categories[categoryIndex],
        name: document.getElementById('categoryName').value,
        description: document.getElementById('categoryDescription').value
    };
    
    saveDataToStorage();
    renderCategories();
    updateCategorySelects();
    closeModal('addCategoryModal');
    document.getElementById('addCategoryForm').reset();
    
    showNotification('Catégorie mise à jour avec succès', 'success');
}

function deleteCategory(id) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette catégorie ?')) {
        categories = categories.filter(c => c.id !== id);
        subCategories = subCategories.filter(sc => sc.parentId !== id);
        saveDataToStorage();
        renderCategories();
        updateCategorySelects();
        showNotification('Catégorie supprimée avec succès', 'success');
    }
}

function editSubCategory(id) {
    const subCategory = subCategories.find(sc => sc.id === id);
    if (!subCategory) return;
    
    document.getElementById('subCategoryName').value = subCategory.name;
    document.getElementById('subCategoryParent').value = subCategory.parentId;
    document.getElementById('subCategoryDescription').value = subCategory.description || '';
    
    const form = document.getElementById('addSubCategoryForm');
    form.onsubmit = function(e) {
        e.preventDefault();
        updateSubCategory(id);
    };
    
    document.querySelector('#addSubCategoryModal .modal-header h2').textContent = 'Modifier la Sous-catégorie';
    showAddSubCategoryModal();
}

function updateSubCategory(id) {
    const subCategoryIndex = subCategories.findIndex(sc => sc.id === id);
    if (subCategoryIndex === -1) return;
    
    subCategories[subCategoryIndex] = {
        ...subCategories[subCategoryIndex],
        name: document.getElementById('subCategoryName').value,
        parentId: parseInt(document.getElementById('subCategoryParent').value),
        description: document.getElementById('subCategoryDescription').value
    };
    
    saveDataToStorage();
    renderCategories();
    updateCategorySelects();
    closeModal('addSubCategoryModal');
    document.getElementById('addSubCategoryForm').reset();
    
    showNotification('Sous-catégorie mise à jour avec succès', 'success');
}

function deleteSubCategory(id) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette sous-catégorie ?')) {
        subCategories = subCategories.filter(sc => sc.id !== id);
        saveDataToStorage();
        renderCategories();
        updateCategorySelects();
        showNotification('Sous-catégorie supprimée avec succès', 'success');
    }
}

// Gestion des alertes et maintenances - SUPPRIMÉ

function loadEnterprisesForAlerts() {
    const select = document.getElementById('alertsEntrepriseSelect');
    if (!select) return;

    fetch('/ent/api/list', {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'same-origin'
    })
    .then(response => {
        if (response.status === 403) {
            // Accès refusé - gérer gracieusement
            console.warn('Accès refusé pour charger les entreprises');
            return [];
        }
        if (!response.ok) {
            throw new Error(`Erreur ${response.status} lors du chargement des entreprises`);
        }
        return response.json();
    })
    .then(enterprises => {
        if (!enterprises || !Array.isArray(enterprises)) {
            enterprises = [];
        }
        
        select.innerHTML = '<option value="">Sélectionner une entreprise</option>';
        enterprises.forEach(entreprise => {
            const option = document.createElement('option');
            option.value = entreprise.entrepriseId || entreprise.id;
            option.textContent = entreprise.nom || entreprise.name || 'Sans nom';
            select.appendChild(option);
        });

        // Sélectionner l'entreprise actuelle si disponible
        const currentEnterprise = JSON.parse(localStorage.getItem('currentEnterprise') || '{}');
        if (currentEnterprise.entrepriseId || currentEnterprise.id) {
            select.value = currentEnterprise.entrepriseId || currentEnterprise.id;
            loadAlertes();
        } else if (enterprises.length > 0) {
            select.value = enterprises[0].entrepriseId || enterprises[0].id;
            loadAlertes();
        }
    })
    .catch(error => {
        console.error('Erreur lors du chargement des entreprises:', error);
        // Ne pas afficher de notification si c'est juste un problème d'accès
        if (!error.message || !error.message.includes('403')) {
            if (typeof showNotification === 'function') {
                showNotification('Erreur lors du chargement des entreprises', 'error');
            }
        }
    });
}

function loadAlertes() {
    const select = document.getElementById('alertsEntrepriseSelect');
    const alertsList = document.getElementById('alertsList');
    const alertsEmpty = document.getElementById('alertsEmpty');
    const alertsLoading = document.getElementById('alertsLoading');
    
    // Vérifier que les éléments DOM existent
    if (!alertsList || !alertsEmpty || !alertsLoading) {
        // Les éléments n'existent pas sur cette page
        return;
    }
    
    const entrepriseId = select ? select.value : '';
    
    if (!entrepriseId) {
        alertsList.innerHTML = '';
        alertsEmpty.style.display = 'block';
        alertsLoading.style.display = 'none';
        return;
    }

    alertsLoading.style.display = 'block';
    alertsEmpty.style.display = 'none';
    alertsList.innerHTML = '';

    fetch(`/alertes/api/list?entrepriseId=${entrepriseId}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'same-origin'
    })
    .then(response => {
        if (response.status === 403) {
            // Accès refusé - l'utilisateur n'est probablement pas superadmin
            throw new Error('Accès refusé. Seul le superadmin peut accéder aux alertes.');
        }
        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: Erreur lors du chargement des alertes`);
        }
        return response.json();
    })
    .then(data => {
        alertes = data || [];
        filteredAlertes = [...alertes];
        displayAlertes();
    })
    .catch(error => {
        console.error('Erreur lors du chargement des alertes:', error);
        alertsLoading.style.display = 'none';
        
        // Afficher un message d'erreur approprié
        if (error.message && error.message.includes('Accès refusé')) {
            alertsEmpty.innerHTML = `
                <i class="fas fa-lock" style="font-size: 3rem; color: var(--text-secondary); margin-bottom: 1rem;"></i>
                <h3 style="color: var(--text-secondary); margin-bottom: 0.5rem;">Accès refusé</h3>
                <p style="color: var(--text-secondary);">Seul le superadmin peut accéder aux alertes.</p>
            `;
            alertsEmpty.style.display = 'block';
        } else if (typeof showNotification === 'function') {
            showNotification('Erreur lors du chargement des alertes: ' + error.message, 'error');
        }
    });
}

function displayAlertes() {
    const container = document.getElementById('alertsList');
    const loading = document.getElementById('alertsLoading');
    const empty = document.getElementById('alertsEmpty');

    // Vérifier que les éléments DOM existent
    if (!container || !loading || !empty) {
        return;
    }

    loading.style.display = 'none';

    if (!filteredAlertes || filteredAlertes.length === 0) {
        empty.style.display = 'block';
        container.innerHTML = '';
        return;
    }

    empty.style.display = 'none';
    container.innerHTML = filteredAlertes.map(alerte => {
        const dateVerification = alerte.dateVerification ? new Date(alerte.dateVerification) : null;
        const isOverdue = dateVerification && dateVerification < new Date() && !alerte.verifie;
        const statusClass = alerte.verifie ? 'verified' : (alerte.envoye ? 'sent' : 'pending');
        const statusText = alerte.verifie ? 'Vérifiée' : (alerte.envoye ? 'Envoyée' : 'En attente');
        const statusIcon = alerte.verifie ? 'fa-check-circle' : (alerte.envoye ? 'fa-paper-plane' : 'fa-clock');

        return `
            <div class="alerte-card ${statusClass} ${isOverdue ? 'overdue' : ''}" data-alerte-id="${alerte.alerteId}">
                <div class="alerte-header">
                    <div>
                        <h3 style="margin: 0 0 0.25rem 0; font-size: 1.1rem; color: var(--text-primary);">
                            <i class="fas fa-cog" style="margin-right: 0.5rem; color: var(--primary-color);"></i>
                            ${alerte.machineNom || 'Machine inconnue'}
                        </h3>
                        <span class="alerte-badge ${statusClass}" style="display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: 600; background: ${getStatusColor(statusClass)}; color: white;">
                            <i class="fas ${statusIcon}"></i> ${statusText}
                        </span>
                    </div>
                </div>
                <div class="alerte-meta" style="margin: 1rem 0; color: var(--text-secondary); font-size: 0.9rem;">
                    ${dateVerification ? `
                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                            <i class="fas fa-calendar-check"></i>
                            <span>Date de vérification: ${formatDate(dateVerification)}</span>
                            ${isOverdue ? '<span style="color: var(--danger-color); font-weight: 600;">⚠ En retard</span>' : ''}
                        </div>
                    ` : ''}
                    ${alerte.creeParNom ? `
                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                            <i class="fas fa-user"></i>
                            <span>Créée par: ${alerte.creeParNom}</span>
                        </div>
                    ` : ''}
                    ${alerte.activerRelance && alerte.nombreRelances > 0 ? `
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <i class="fas fa-redo"></i>
                            <span>Relances: ${alerte.nombreRelancesEnvoyees || 0}/${alerte.nombreRelances}</span>
                        </div>
                    ` : ''}
                </div>
                ${alerte.description ? `
                    <div class="alerte-description" style="margin: 1rem 0; padding: 0.75rem; background: var(--bg-secondary); border-radius: 6px; color: var(--text-primary);">
                        ${alerte.description}
                    </div>
                ` : ''}
                ${alerte.verifie && alerte.dateVerificationReelle ? `
                    <div style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--border-color); color: var(--success-color); font-size: 0.9rem;">
                        <i class="fas fa-check-circle"></i> Vérifiée le ${formatDate(new Date(alerte.dateVerificationReelle))}
                    </div>
                ` : ''}
                <div class="alerte-actions" style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--border-color); display: flex; gap: 0.5rem; flex-wrap: wrap;">
                    ${!alerte.verifie ? `
                        <button class="btn btn-sm btn-success" onclick="marquerAlerteVerifiee('${alerte.entrepriseId}', '${alerte.alerteId}')" style="padding: 0.5rem 1rem; font-size: 0.85rem;">
                            <i class="fas fa-check"></i> Vérifier
                        </button>
                    ` : ''}
                    <button class="btn btn-sm btn-warning" onclick="editAlerte('${alerte.entrepriseId}', '${alerte.alerteId}')" style="padding: 0.5rem 1rem; font-size: 0.85rem;">
                        <i class="fas fa-edit"></i> Modifier
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteAlerteConfirm('${alerte.entrepriseId}', '${alerte.alerteId}')" style="padding: 0.5rem 1rem; font-size: 0.85rem;">
                        <i class="fas fa-trash"></i> Supprimer
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function getStatusColor(status) {
    switch(status) {
        case 'verified': return '#10b981';
        case 'sent': return '#3b82f6';
        case 'pending': return '#f59e0b';
        default: return '#6b7280';
    }
}

function formatDate(date) {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function setupAlertsFilters() {
    const searchInput = document.getElementById('alertsSearch');
    const statusFilter = document.getElementById('alertsStatusFilter');
    const dateFilter = document.getElementById('alertsDateFilter');
    const entrepriseSelect = document.getElementById('alertsEntrepriseSelect');

    // Vérifier que les éléments existent avant d'ajouter les listeners
    if (searchInput) {
        // Retirer l'ancien listener s'il existe pour éviter les doublons
        searchInput.removeEventListener('input', applyAlertsFilters);
        searchInput.addEventListener('input', applyAlertsFilters);
    }
    if (statusFilter) {
        statusFilter.removeEventListener('change', applyAlertsFilters);
        statusFilter.addEventListener('change', applyAlertsFilters);
    }
    if (dateFilter) {
        dateFilter.removeEventListener('change', applyAlertsFilters);
        dateFilter.addEventListener('change', applyAlertsFilters);
    }
    if (entrepriseSelect) {
        entrepriseSelect.removeEventListener('change', loadAlertes);
        entrepriseSelect.addEventListener('change', loadAlertes);
    }
}

function applyAlertsFilters() {
    const searchTerm = (document.getElementById('alertsSearch')?.value || '').toLowerCase();
    const statusFilter = document.getElementById('alertsStatusFilter')?.value || 'all';
    const dateFilter = document.getElementById('alertsDateFilter')?.value || 'all';
    const now = new Date();

    filteredAlertes = alertes.filter(alerte => {
        // Filtre de recherche
        if (searchTerm) {
            const searchIn = `${alerte.machineNom || ''} ${alerte.description || ''}`.toLowerCase();
            if (!searchIn.includes(searchTerm)) return false;
        }

        // Filtre de statut
        if (statusFilter !== 'all') {
            if (statusFilter === 'pending' && (alerte.verifie || alerte.envoye)) return false;
            if (statusFilter === 'sent' && !alerte.envoye) return false;
            if (statusFilter === 'verified' && !alerte.verifie) return false;
        }

        // Filtre de date
        if (dateFilter !== 'all' && alerte.dateVerification) {
            const alertDate = new Date(alerte.dateVerification);
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            const alertDay = new Date(alertDate.getFullYear(), alertDate.getMonth(), alertDate.getDate());

            if (dateFilter === 'today' && alertDay.getTime() !== today.getTime()) return false;
            if (dateFilter === 'week') {
                const weekAgo = new Date(today);
                weekAgo.setDate(weekAgo.getDate() - 7);
                if (alertDate < weekAgo) return false;
            }
            if (dateFilter === 'month') {
                const monthAgo = new Date(today);
                monthAgo.setMonth(monthAgo.getMonth() - 1);
                if (alertDate < monthAgo) return false;
            }
            if (dateFilter === 'overdue' && (alertDate >= now || alerte.verifie)) return false;
        }

        return true;
    });

    displayAlertes();
}

// Fonction pour afficher le modal d'ajout d'alerte
async function showAddAlertModal() {
    // Vérifier si l'utilisateur est superadmin
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut créer des alertes.', 'error');
        return;
    }

    const entrepriseSelect = document.getElementById('alertsEntrepriseSelect');
    const entrepriseId = entrepriseSelect ? entrepriseSelect.value : '';
    
    if (!entrepriseId) {
        showNotification('Veuillez sélectionner une entreprise d\'abord', 'warning');
        return;
    }

    const alertMachineSelect = document.getElementById('alertMachine');
    if (!alertMachineSelect) {
        showNotification('Erreur: Le formulaire d\'alerte est introuvable', 'error');
        return;
    }

    // Charger les machines de l'entreprise via une requête vers la page machines
    try {
        // Essayer de charger depuis l'endpoint machines avec entrepriseId
        const response = await fetch(`/machines?entrepriseId=${entrepriseId}`, {
            method: 'GET',
            headers: {
                'Accept': 'text/html'
            },
            credentials: 'same-origin'
        });
        
        if (response.ok) {
            const html = await response.text();
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');
            
            // Extraire les machines depuis le HTML
            const machineOptions = doc.querySelectorAll('select[name="machineId"] option, select[id*="machine"] option');
            
    alertMachineSelect.innerHTML = '<option value="">Sélectionner une machine</option>';
    
        if (machineOptions.length > 0) {
            machineOptions.forEach(option => {
                if (option.value && option.value !== '') {
                    const newOption = document.createElement('option');
                    newOption.value = option.value;
                    newOption.textContent = option.textContent.trim();
                    alertMachineSelect.appendChild(newOption);
                }
            });
        } else {
            // Si pas de machines trouvées dans le HTML, essayer une autre méthode
            // Utiliser l'API Firebase directement via un endpoint si disponible
            showNotification('Aucune machine trouvée pour cette entreprise', 'warning');
        }
        } else {
            // Fallback: utiliser les machines déjà chargées dans la page si disponibles
            alertMachineSelect.innerHTML = '<option value="">Sélectionner une machine</option>';
            showNotification('Veuillez vous assurer que l\'entreprise a des machines', 'info');
        }
        
        // Réinitialiser le formulaire
        const form = document.getElementById('addAlertForm');
        if (form) {
            form.reset();
            // Définir l'entrepriseId dans un champ caché
            let hiddenInput = form.querySelector('input[name="entrepriseId"]');
            if (!hiddenInput) {
                hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.name = 'entrepriseId';
                form.appendChild(hiddenInput);
            }
            hiddenInput.value = entrepriseId;
            
            // Réinitialiser le toggle des relances
            const relanceCheckbox = document.getElementById('alertActiverRelance');
            const relanceGroup = document.getElementById('addRelancesGroup');
            if (relanceCheckbox && relanceGroup) {
                relanceCheckbox.checked = false;
                relanceGroup.style.display = 'none';
            }
        }
    
    document.getElementById('addAlertModal').classList.add('show');
    } catch (error) {
        console.error('Erreur lors du chargement des machines:', error);
        // Afficher le modal quand même, l'utilisateur pourra sélectionner manuellement
        alertMachineSelect.innerHTML = '<option value="">Sélectionner une machine</option>';
        document.getElementById('addAlertModal').classList.add('show');
        showNotification('Erreur lors du chargement des machines. Veuillez recharger la page.', 'error');
    }
}

// Fonction pour créer une nouvelle alerte
async function handleAddAlert(e) {
    e.preventDefault();
    
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut créer des alertes.', 'error');
        return;
    }

    const form = e.target;
    const formData = new FormData(form);
    
    // Récupérer l'entrepriseId depuis le select ou le champ caché
    const entrepriseSelect = document.getElementById('alertsEntrepriseSelect');
    const entrepriseId = formData.get('entrepriseId') || (entrepriseSelect ? entrepriseSelect.value : '');
    const machineId = formData.get('machineId') || document.getElementById('alertMachine')?.value;
    const dateVerification = formData.get('dateVerification') || document.getElementById('alertDate')?.value;
    const description = formData.get('description') || document.getElementById('alertDescription')?.value || '';
    const activerRelance = formData.get('activerRelance') === 'on' || document.getElementById('alertActiverRelance')?.checked || false;
    const nombreRelances = formData.get('nombreRelances') || document.getElementById('alertNombreRelances')?.value || '0';

    if (!entrepriseId) {
        showNotification('Veuillez sélectionner une entreprise', 'error');
        return;
    }

    if (!machineId) {
        showNotification('Veuillez sélectionner une machine', 'error');
        return;
    }

    if (!dateVerification) {
        showNotification('Veuillez sélectionner une date de vérification', 'error');
        return;
    }

    // Créer un formulaire pour soumettre via POST
    const submitForm = document.createElement('form');
    submitForm.method = 'POST';
    submitForm.action = '/alertes';
    
    const fields = {
        entrepriseId: entrepriseId,
        machineId: machineId,
        dateVerification: dateVerification,
        description: description
    };

    // Ajouter activerRelance seulement si coché
    if (activerRelance) {
        fields.activerRelance = 'on';
        fields.nombreRelances = nombreRelances;
    }

    Object.entries(fields).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = key;
            input.value = value;
            submitForm.appendChild(input);
        }
    });

    document.body.appendChild(submitForm);
    submitForm.submit();
}

// Fonction pour marquer une alerte comme vérifiée
async function marquerAlerteVerifiee(entrepriseId, alerteId) {
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut marquer une alerte comme vérifiée.', 'error');
        return;
    }

    if (!confirm('Êtes-vous sûr de vouloir marquer cette machine comme vérifiée ?')) {
        return;
    }

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/alertes/${entrepriseId}/${alerteId}/verifie`;
    document.body.appendChild(form);
    form.submit();
}

// Fonction pour éditer une alerte
async function editAlerte(entrepriseId, alerteId) {
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut modifier une alerte.', 'error');
        return;
    }

    // Charger les détails de l'alerte
    try {
        const response = await fetch(`/alertes/api/list?entrepriseId=${entrepriseId}`);
        const data = await response.json();
        const alerte = data.find(a => a.alerteId === alerteId);
        
        if (!alerte) {
            showNotification('Alerte introuvable', 'error');
            return;
        }

        // Créer ou afficher le modal d'édition
        showEditAlerteModal(alerte, entrepriseId);
    } catch (error) {
        console.error('Erreur lors du chargement de l\'alerte:', error);
        showNotification('Erreur lors du chargement de l\'alerte', 'error');
    }
}

// Fonction pour afficher le modal d'édition
function showEditAlerteModal(alerte, entrepriseId) {
    // Créer le modal s'il n'existe pas
    let editModal = document.getElementById('editAlerteModal');
    if (!editModal) {
        editModal = createEditAlerteModal();
    }

    // Remplir le formulaire
    const dateVerification = alerte.dateVerification ? new Date(alerte.dateVerification).toISOString().split('T')[0] : '';
    
    document.getElementById('editAlerteId').value = alerte.alerteId;
    document.getElementById('editAlerteEntrepriseId').value = entrepriseId;
    document.getElementById('editAlerteDateVerification').value = dateVerification;
    document.getElementById('editAlerteDescription').value = alerte.description || '';
    document.getElementById('editAlerteActiverRelance').checked = alerte.activerRelance || false;
    document.getElementById('editAlerteNombreRelances').value = alerte.nombreRelances || 0;
    
    toggleRelancesEdit();
    
    editModal.classList.add('show');
}

// Fonction pour supprimer une alerte
async function deleteAlerteConfirm(entrepriseId, alerteId) {
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut supprimer une alerte.', 'error');
        return;
    }

    if (!confirm('Êtes-vous sûr de vouloir supprimer cette alerte ? Cette action est irréversible.')) {
        return;
    }

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = `/alertes/${entrepriseId}/${alerteId}/delete`;
    document.body.appendChild(form);
    form.submit();
}

// Fonction pour créer le modal d'édition d'alerte
function createEditAlerteModal() {
    const modal = document.createElement('div');
    modal.id = 'editAlerteModal';
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h2>Modifier l'Alerte</h2>
                <span class="close" onclick="closeModal('editAlerteModal')">&times;</span>
            </div>
            <form id="editAlerteForm" onsubmit="handleEditAlerte(event)">
                <input type="hidden" id="editAlerteId" name="alerteId">
                <input type="hidden" id="editAlerteEntrepriseId" name="entrepriseId">
                <div class="form-group">
                    <label for="editAlerteDateVerification">Date de vérification *</label>
                    <input type="date" id="editAlerteDateVerification" name="dateVerification" required>
                </div>
                <div class="form-group">
                    <label for="editAlerteDescription">Description</label>
                    <textarea id="editAlerteDescription" name="description" placeholder="Description de la vérification à effectuer..."></textarea>
                </div>
                <div class="form-group">
                    <label style="display: flex; align-items: center; gap: 0.5rem;">
                        <input type="checkbox" id="editAlerteActiverRelance" name="activerRelance" onchange="toggleRelancesEdit()">
                        Activer les relances
                    </label>
                </div>
                <div class="form-group" id="editRelancesGroup" style="display: none;">
                    <label for="editAlerteNombreRelances">Nombre de relances</label>
                    <input type="number" id="editAlerteNombreRelances" name="nombreRelances" min="0" max="10" value="0" placeholder="Ex: 3">
                    <small style="color: #6b7280; font-size: 0.85rem;">Les relances seront envoyées toutes les 24h si la machine n'est pas vérifiée</small>
                </div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="closeModal('editAlerteModal')">Annuler</button>
                    <button type="submit" class="btn btn-primary">Modifier</button>
                </div>
            </form>
        </div>
    `;
    document.body.appendChild(modal);
    return modal;
}

// Fonction pour gérer la soumission du formulaire d'édition
async function handleEditAlerte(e) {
    e.preventDefault();
    
    const isSuperAdmin = await checkIfSuperAdmin();
    if (!isSuperAdmin) {
        showNotification('Accès refusé. Seul le superadmin peut modifier une alerte.', 'error');
        return;
    }

    const form = e.target;
    const formData = new FormData(form);
    const entrepriseId = formData.get('entrepriseId');
    const alerteId = formData.get('alerteId');
    const dateVerification = formData.get('dateVerification');
    const description = formData.get('description') || '';
    const activerRelance = formData.get('activerRelance') === 'on';
    const nombreRelances = formData.get('nombreRelances') || '0';

    if (!entrepriseId || !alerteId || !dateVerification) {
        showNotification('Veuillez remplir tous les champs obligatoires', 'error');
        return;
    }

    // Créer un formulaire pour soumettre via POST
    const submitForm = document.createElement('form');
    submitForm.method = 'POST';
    submitForm.action = `/alertes/${entrepriseId}/${alerteId}`;
    
    const fields = {
        dateVerification: dateVerification,
        description: description,
        activerRelance: activerRelance ? 'on' : '',
        nombreRelances: nombreRelances
    };

    Object.entries(fields).forEach(([key, value]) => {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        submitForm.appendChild(input);
    });

    document.body.appendChild(submitForm);
    submitForm.submit();
}

// Fonction pour toggle les relances dans le formulaire d'édition
function toggleRelancesEdit() {
    const checkbox = document.getElementById('editAlerteActiverRelance');
    const group = document.getElementById('editRelancesGroup');
    if (checkbox && group) {
        group.style.display = checkbox.checked ? 'block' : 'none';
    }
}

// Fonction pour toggle les relances dans le formulaire d'ajout
function toggleRelancesAdd() {
    const checkbox = document.getElementById('alertActiverRelance');
    const group = document.getElementById('addRelancesGroup');
    if (checkbox && group) {
        group.style.display = checkbox.checked ? 'block' : 'none';
    }
}

// Fonction pour créer le modal d'alerte si nécessaire
function createAlerteModal() {
    // Le modal existe déjà dans index.html, pas besoin de le créer
    showNotification('Veuillez sélectionner une entreprise d\'abord', 'warning');
}


function completeMaintenance(alertId) {
    const alert = maintenanceAlerts.find(a => a.id === alertId);
    if (!alert) return;
    
    const machine = machines.find(m => m.id === alert.machineId);
    const technician = prompt('Nom du technicien qui a effectué la maintenance:');
    
    if (technician) {
        // Ajouter à l'historique
        maintenanceHistory.unshift({
            id: Date.now(),
            machineId: alert.machineId,
            machineName: machine ? machine.name : 'Machine supprimée',
            date: new Date().toISOString(),
            description: alert.description,
            technician: technician
        });
        
        // Marquer l'alerte comme terminée
        alert.completed = true;
        alert.completedAt = new Date().toISOString();
        alert.technician = technician;
        
        // Programmer la prochaine maintenance si une fréquence est définie
        if (alert.frequency && alert.frequency !== 'custom') {
            scheduleNextMaintenance(alert);
        }
        
        saveDataToStorage();
        updateDashboard();
        showNotification('Maintenance marquée comme terminée', 'success');
    }
}

function scheduleNextMaintenance(alert) {
    const nextDate = new Date(alert.date);
    
    switch(alert.frequency) {
        case 'monthly':
            nextDate.setMonth(nextDate.getMonth() + 1);
            break;
        case 'quarterly':
            nextDate.setMonth(nextDate.getMonth() + 3);
            break;
        case 'biannually':
            nextDate.setMonth(nextDate.getMonth() + 6);
            break;
        case 'annually':
            nextDate.setFullYear(nextDate.getFullYear() + 1);
            break;
    }
    
    const nextAlert = {
        id: Date.now(),
        machineId: alert.machineId,
        date: nextDate.toISOString().split('T')[0],
        time: alert.time,
        description: alert.description,
        frequency: alert.frequency,
        completed: false,
        createdAt: new Date()
    };
    
    maintenanceAlerts.push(nextAlert);
}

function rescheduleMaintenance(alertId) {
    const alert = maintenanceAlerts.find(a => a.id === alertId);
    if (!alert) return;
    
    const newDate = prompt('Nouvelle date de maintenance (YYYY-MM-DD):', alert.date);
    if (newDate) {
        alert.date = newDate;
        saveDataToStorage();
        updateDashboard();
        showNotification('Maintenance reprogrammée', 'success');
    }
}

function deleteAlert(alertId) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette alerte ?')) {
        maintenanceAlerts = maintenanceAlerts.filter(a => a.id !== alertId);
        saveDataToStorage();
        updateDashboard();
        showNotification('Alerte supprimée', 'success');
    }
}

// Gestion des réparations
function renderRepairs() {
    const activeContainer = document.getElementById('activeRepairs');
    const historyContainer = document.getElementById('repairHistory');
    
    // Réparations actives (en attente et en cours)
    const activeRepairs = repairs.filter(repair => 
        repair.status === 'pending' || repair.status === 'in_progress'
    );
    
    activeContainer.innerHTML = activeRepairs.map(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        const priorityClass = repair.priority === 'urgent' ? 'urgent' : '';
        
        return `
            <div class="repair-item ${repair.status} ${priorityClass}">
                <div class="repair-header">
                    <span class="repair-title">${repair.title}</span>
                    <span class="repair-priority ${repair.priority}">${getPriorityText(repair.priority)}</span>
                </div>
                <div class="repair-machine">
                    <i class="fas fa-cogs"></i> ${machine ? machine.name : 'Machine supprimée'} (${machine ? machine.location : 'N/A'})
                </div>
                <div class="repair-description">${repair.description}</div>
                <div class="repair-meta">
                    <span><i class="fas fa-user"></i> ${repair.technician || 'Non assigné'}</span>
                    <span><i class="fas fa-clock"></i> ${repair.estimatedDuration}h estimées</span>
                    <span><i class="fas fa-euro-sign"></i> ${repair.estimatedCost}€ estimé</span>
                </div>
                <div class="repair-actions">
                    <button class="btn btn-sm btn-primary" onclick="showRepairDetails(${repair.id})">
                        <i class="fas fa-eye"></i> Détails
                    </button>
                    ${repair.status === 'pending' ? `
                        <button class="btn btn-sm btn-success" onclick="startRepair(${repair.id})">
                            <i class="fas fa-play"></i> Démarrer
                        </button>
                    ` : ''}
                    ${repair.status === 'in_progress' ? `
                        <button class="btn btn-sm btn-success" onclick="completeRepair(${repair.id})">
                            <i class="fas fa-check"></i> Terminer
                        </button>
                    ` : ''}
                    <button class="btn btn-sm btn-warning" onclick="editRepair(${repair.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="cancelRepair(${repair.id})">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
    
    // Historique des réparations (terminées et annulées)
    const completedRepairs = repairs.filter(repair => 
        repair.status === 'completed' || repair.status === 'cancelled'
    );
    
    historyContainer.innerHTML = completedRepairs.map(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        
        return `
            <div class="repair-item ${repair.status}">
                <div class="repair-header">
                    <span class="repair-title">${repair.title}</span>
                    <span class="repair-status-badge ${repair.status}">${getStatusText(repair.status)}</span>
                </div>
                <div class="repair-machine">
                    <i class="fas fa-cogs"></i> ${machine ? machine.name : 'Machine supprimée'} (${machine ? machine.location : 'N/A'})
                </div>
                <div class="repair-description">${repair.description}</div>
                <div class="repair-meta">
                    <span><i class="fas fa-user"></i> ${repair.technician || 'Non assigné'}</span>
                    <span><i class="fas fa-clock"></i> ${repair.actualDuration || repair.estimatedDuration}h</span>
                    <span><i class="fas fa-euro-sign"></i> ${repair.actualCost || repair.estimatedCost}€</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(repair.completedAt || repair.createdAt)}</span>
                </div>
                <div class="repair-actions">
                    <button class="btn btn-sm btn-primary" onclick="showRepairDetails(${repair.id})">
                        <i class="fas fa-eye"></i> Détails
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function showAddRepairModal() {
    const repairMachineSelect = document.getElementById('repairMachine');
    repairMachineSelect.innerHTML = '<option value="">Sélectionner une machine</option>';
    
    machines.forEach(machine => {
        const option = document.createElement('option');
        option.value = machine.id;
        option.textContent = `${machine.name} (${machine.location})`;
        repairMachineSelect.appendChild(option);
    });
    
    document.getElementById('addRepairModal').classList.add('show');
}

function handleAddRepair(e) {
    e.preventDefault();
    
    const repair = {
        id: Date.now(),
        machineId: parseInt(document.getElementById('repairMachine').value),
        title: document.getElementById('repairTitle').value,
        description: document.getElementById('repairDescription').value,
        priority: document.getElementById('repairPriority').value,
        status: 'pending',
        technician: document.getElementById('repairTechnician').value,
        estimatedCost: parseFloat(document.getElementById('repairEstimatedCost').value) || 0,
        actualCost: 0,
        estimatedDuration: parseFloat(document.getElementById('repairEstimatedDuration').value) || 0,
        actualDuration: 0,
        createdAt: new Date(),
        startedAt: null,
        completedAt: null,
        notes: ''
    };
    
    repairs.push(repair);
    saveDataToStorage();
    renderRepairs();
    updateDashboard();
    closeModal('addRepairModal');
    document.getElementById('addRepairForm').reset();
    
    showNotification('Réparation créée avec succès', 'success');
}

function showRepairDetails(repairId) {
    const repair = repairs.find(r => r.id === repairId);
    if (!repair) return;
    
    const machine = machines.find(m => m.id === repair.machineId);
    const content = document.getElementById('repairDetailsContent');
    
    content.innerHTML = `
        <div class="repair-details">
            <div class="detail-section">
                <h3>Informations générales</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>Titre:</label>
                        <span>${repair.title}</span>
                    </div>
                    <div class="detail-item">
                        <label>Machine:</label>
                        <span>${machine ? machine.name : 'Machine supprimée'} (${machine ? machine.location : 'N/A'})</span>
                    </div>
                    <div class="detail-item">
                        <label>Statut:</label>
                        <span class="repair-status-badge ${repair.status}">${getStatusText(repair.status)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Priorité:</label>
                        <span class="repair-priority ${repair.priority}">${getPriorityText(repair.priority)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Technicien:</label>
                        <span>${repair.technician || 'Non assigné'}</span>
                    </div>
                </div>
            </div>
            
            <div class="detail-section">
                <h3>Description du problème</h3>
                <p>${repair.description}</p>
            </div>
            
            <div class="detail-section">
                <h3>Coûts et durée</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>Coût estimé:</label>
                        <span>${repair.estimatedCost}€</span>
                    </div>
                    <div class="detail-item">
                        <label>Coût réel:</label>
                        <span>${repair.actualCost || 'Non défini'}€</span>
                    </div>
                    <div class="detail-item">
                        <label>Durée estimée:</label>
                        <span>${repair.estimatedDuration}h</span>
                    </div>
                    <div class="detail-item">
                        <label>Durée réelle:</label>
                        <span>${repair.actualDuration || 'Non définie'}h</span>
                    </div>
                </div>
            </div>
            
            <div class="detail-section">
                <h3>Dates</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>Créée le:</label>
                        <span>${formatDate(repair.createdAt)}</span>
                    </div>
                    ${repair.startedAt ? `
                        <div class="detail-item">
                            <label>Démarrée le:</label>
                            <span>${formatDate(repair.startedAt)}</span>
                        </div>
                    ` : ''}
                    ${repair.completedAt ? `
                        <div class="detail-item">
                            <label>Terminée le:</label>
                            <span>${formatDate(repair.completedAt)}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
            
            ${repair.notes ? `
                <div class="detail-section">
                    <h3>Notes</h3>
                    <p>${repair.notes}</p>
                </div>
            ` : ''}
        </div>
    `;
    
    document.getElementById('repairDetailsModal').classList.add('show');
}

function startRepair(repairId) {
    const repairIndex = repairs.findIndex(r => r.id === repairId);
    if (repairIndex === -1) return;
    
    repairs[repairIndex].status = 'in_progress';
    repairs[repairIndex].startedAt = new Date();
    
    saveDataToStorage();
    renderRepairs();
    updateDashboard();
    showNotification('Réparation démarrée', 'success');
}

function completeRepair(repairId) {
    const repair = repairs.find(r => r.id === repairId);
    if (!repair) return;
    
    const actualCost = prompt('Coût réel de la réparation (€):', repair.actualCost || repair.estimatedCost);
    const actualDuration = prompt('Durée réelle de la réparation (heures):', repair.actualDuration || repair.estimatedDuration);
    const notes = prompt('Notes sur la réparation (optionnel):', repair.notes || '');
    
    if (actualCost !== null && actualDuration !== null) {
        const repairIndex = repairs.findIndex(r => r.id === repairId);
        
        repairs[repairIndex].status = 'completed';
        repairs[repairIndex].completedAt = new Date();
        repairs[repairIndex].actualCost = parseFloat(actualCost);
        repairs[repairIndex].actualDuration = parseFloat(actualDuration);
        repairs[repairIndex].notes = notes;
        
        // Ajouter à l'historique
        repairHistory.unshift({
            id: Date.now(),
            repairId: repairId,
            machineId: repair.machineId,
            title: repair.title,
            technician: repair.technician,
            completedAt: new Date(),
            actualCost: parseFloat(actualCost),
            actualDuration: parseFloat(actualDuration),
            notes: notes
        });
        
        saveDataToStorage();
        renderRepairs();
        updateDashboard();
        showNotification('Réparation terminée avec succès', 'success');
    }
}

function editRepair(repairId) {
    const repair = repairs.find(r => r.id === repairId);
    if (!repair) return;
    
    // Remplir le formulaire avec les données existantes
    document.getElementById('repairMachine').value = repair.machineId;
    document.getElementById('repairTitle').value = repair.title;
    document.getElementById('repairDescription').value = repair.description;
    document.getElementById('repairPriority').value = repair.priority;
    document.getElementById('repairTechnician').value = repair.technician;
    document.getElementById('repairEstimatedCost').value = repair.estimatedCost;
    document.getElementById('repairEstimatedDuration').value = repair.estimatedDuration;
    
    // Mettre à jour le titre du modal
    document.querySelector('#addRepairModal .modal-header h2').textContent = 'Modifier la Réparation';
    
    // Changer l'action du formulaire
    const form = document.getElementById('addRepairForm');
    form.onsubmit = function(e) {
        e.preventDefault();
        updateRepair(repairId);
    };
    
    showAddRepairModal();
}

function updateRepair(repairId) {
    const repairIndex = repairs.findIndex(r => r.id === repairId);
    if (repairIndex === -1) return;
    
    repairs[repairIndex] = {
        ...repairs[repairIndex],
        machineId: parseInt(document.getElementById('repairMachine').value),
        title: document.getElementById('repairTitle').value,
        description: document.getElementById('repairDescription').value,
        priority: document.getElementById('repairPriority').value,
        technician: document.getElementById('repairTechnician').value,
        estimatedCost: parseFloat(document.getElementById('repairEstimatedCost').value) || 0,
        estimatedDuration: parseFloat(document.getElementById('repairEstimatedDuration').value) || 0
    };
    
    saveDataToStorage();
    renderRepairs();
    updateDashboard();
    closeModal('addRepairModal');
    document.getElementById('addRepairForm').reset();
    
    showNotification('Réparation mise à jour avec succès', 'success');
}

function cancelRepair(repairId) {
    if (confirm('Êtes-vous sûr de vouloir annuler cette réparation ?')) {
        const repairIndex = repairs.findIndex(r => r.id === repairId);
        if (repairIndex === -1) return;
        
        repairs[repairIndex].status = 'cancelled';
        repairs[repairIndex].completedAt = new Date();
        
        saveDataToStorage();
        renderRepairs();
        updateDashboard();
        showNotification('Réparation annulée', 'success');
    }
}

function filterRepairs() {
    const searchTerm = document.getElementById('repairSearch').value.toLowerCase();
    const statusFilter = document.getElementById('repairStatusFilter').value;
    const priorityFilter = document.getElementById('repairPriorityFilter').value;
    
    const filteredRepairs = repairs.filter(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        const matchesSearch = repair.title.toLowerCase().includes(searchTerm) ||
                            repair.description.toLowerCase().includes(searchTerm) ||
                            (machine && machine.name.toLowerCase().includes(searchTerm));
        const matchesStatus = !statusFilter || repair.status === statusFilter;
        const matchesPriority = !priorityFilter || repair.priority === priorityFilter;
        
        return matchesSearch && matchesStatus && matchesPriority;
    });
    
    // Re-rendre avec les réparations filtrées
    renderFilteredRepairs(filteredRepairs);
}

function renderFilteredRepairs(filteredRepairs) {
    const activeContainer = document.getElementById('activeRepairs');
    const historyContainer = document.getElementById('repairHistory');
    
    // Réparations actives filtrées
    const activeRepairs = filteredRepairs.filter(repair => 
        repair.status === 'pending' || repair.status === 'in_progress'
    );
    
    activeContainer.innerHTML = activeRepairs.map(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        const priorityClass = repair.priority === 'urgent' ? 'urgent' : '';
        
        return `
            <div class="repair-item ${repair.status} ${priorityClass}">
                <div class="repair-header">
                    <span class="repair-title">${repair.title}</span>
                    <span class="repair-priority ${repair.priority}">${getPriorityText(repair.priority)}</span>
                </div>
                <div class="repair-machine">
                    <i class="fas fa-cogs"></i> ${machine ? machine.name : 'Machine supprimée'} (${machine ? machine.location : 'N/A'})
                </div>
                <div class="repair-description">${repair.description}</div>
                <div class="repair-meta">
                    <span><i class="fas fa-user"></i> ${repair.technician || 'Non assigné'}</span>
                    <span><i class="fas fa-clock"></i> ${repair.estimatedDuration}h estimées</span>
                    <span><i class="fas fa-euro-sign"></i> ${repair.estimatedCost}€ estimé</span>
                </div>
                <div class="repair-actions">
                    <button class="btn btn-sm btn-primary" onclick="showRepairDetails(${repair.id})">
                        <i class="fas fa-eye"></i> Détails
                    </button>
                    ${repair.status === 'pending' ? `
                        <button class="btn btn-sm btn-success" onclick="startRepair(${repair.id})">
                            <i class="fas fa-play"></i> Démarrer
                        </button>
                    ` : ''}
                    ${repair.status === 'in_progress' ? `
                        <button class="btn btn-sm btn-success" onclick="completeRepair(${repair.id})">
                            <i class="fas fa-check"></i> Terminer
                        </button>
                    ` : ''}
                    <button class="btn btn-sm btn-warning" onclick="editRepair(${repair.id})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="cancelRepair(${repair.id})">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
    
    // Historique filtré
    const completedRepairs = filteredRepairs.filter(repair => 
        repair.status === 'completed' || repair.status === 'cancelled'
    );
    
    historyContainer.innerHTML = completedRepairs.map(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        
        return `
            <div class="repair-item ${repair.status}">
                <div class="repair-header">
                    <span class="repair-title">${repair.title}</span>
                    <span class="repair-status-badge ${repair.status}">${getStatusText(repair.status)}</span>
                </div>
                <div class="repair-machine">
                    <i class="fas fa-cogs"></i> ${machine ? machine.name : 'Machine supprimée'} (${machine ? machine.location : 'N/A'})
                </div>
                <div class="repair-description">${repair.description}</div>
                <div class="repair-meta">
                    <span><i class="fas fa-user"></i> ${repair.technician || 'Non assigné'}</span>
                    <span><i class="fas fa-clock"></i> ${repair.actualDuration || repair.estimatedDuration}h</span>
                    <span><i class="fas fa-euro-sign"></i> ${repair.actualCost || repair.estimatedCost}€</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(repair.completedAt || repair.createdAt)}</span>
                </div>
                <div class="repair-actions">
                    <button class="btn btn-sm btn-primary" onclick="showRepairDetails(${repair.id})">
                        <i class="fas fa-eye"></i> Détails
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function getPriorityText(priority) {
    const priorities = {
        low: 'Faible',
        medium: 'Moyenne',
        high: 'Haute',
        urgent: 'Urgente'
    };
    return priorities[priority] || priority;
}

function getStatusText(status) {
    const statuses = {
        pending: 'En attente',
        in_progress: 'En cours',
        completed: 'Terminée',
        cancelled: 'Annulée'
    };
    return statuses[status] || status;
}

// Rapports et Statistiques
function renderReports() {
    updateReportsSummary();
    renderCharts();
}

function updateReportsSummary() {
    const totalMaintenanceCost = maintenanceHistory.reduce((sum, maintenance) => sum + (maintenance.cost || 0), 0);
    const totalRepairCost = repairs.filter(r => r.status === 'completed').reduce((sum, repair) => sum + (repair.actualCost || 0), 0);
    const completedRepairs = repairs.filter(r => r.status === 'completed');
    const avgRepairTime = completedRepairs.length > 0 ? 
        completedRepairs.reduce((sum, repair) => sum + (repair.actualDuration || 0), 0) / completedRepairs.length : 0;
    
    const totalMachinesCount = machines.length;
    const machinesInMaintenance = repairs.filter(r => r.status === 'in_progress').length;
    const availability = totalMachinesCount > 0 ? ((totalMachinesCount - machinesInMaintenance) / totalMachinesCount * 100).toFixed(1) : 0;
    
    document.getElementById('totalMaintenanceCost').textContent = totalMaintenanceCost.toFixed(2) + '€';
    document.getElementById('totalRepairCost').textContent = totalRepairCost.toFixed(2) + '€';
    document.getElementById('avgRepairTime').textContent = avgRepairTime.toFixed(1) + 'h';
    document.getElementById('machineAvailability').textContent = availability + '%';
}

function renderCharts() {
    renderCostsChart();
    renderTechnicianChart();
    renderMachinesChart();
    renderTrendsChart();
}

function renderCostsChart() {
    const ctx = document.getElementById('costsChart').getContext('2d');
    const maintenanceCost = maintenanceHistory.reduce((sum, m) => sum + (m.cost || 0), 0);
    const repairCost = repairs.filter(r => r.status === 'completed').reduce((sum, r) => sum + (r.actualCost || 0), 0);
    
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Maintenance', 'Réparations'],
            datasets: [{
                data: [maintenanceCost, repairCost],
                backgroundColor: ['#10b981', '#f59e0b'],
                borderWidth: 2,
                borderColor: '#fff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

function renderTechnicianChart() {
    const ctx = document.getElementById('technicianChart').getContext('2d');
    const technicianStats = technicians.map(tech => ({
        name: tech.name,
        repairs: repairs.filter(r => r.technician === tech.name && r.status === 'completed').length
    }));
    
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: technicianStats.map(t => t.name),
            datasets: [{
                label: 'Réparations terminées',
                data: technicianStats.map(t => t.repairs),
                backgroundColor: '#2563eb',
                borderColor: '#1d4ed8',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function renderMachinesChart() {
    const ctx = document.getElementById('machinesChart').getContext('2d');
    const machineStats = machines.map(machine => ({
        name: machine.name,
        repairs: repairs.filter(r => r.machineId === machine.id).length
    })).sort((a, b) => b.repairs - a.repairs).slice(0, 5);
    
    new Chart(ctx, {
        type: 'horizontalBar',
        data: {
            labels: machineStats.map(m => m.name),
            datasets: [{
                label: 'Nombre de réparations',
                data: machineStats.map(m => m.repairs),
                backgroundColor: '#ef4444',
                borderColor: '#dc2626',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    beginAtZero: true
                }
            }
        }
    });
}

function renderTrendsChart() {
    const ctx = document.getElementById('trendsChart').getContext('2d');
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];
    const currentMonth = new Date().getMonth();
    const last12Months = months.slice(-12);
    
    // Données simulées pour la démo
    const maintenanceData = [2, 3, 1, 4, 2, 3, 5, 2, 3, 4, 2, 3];
    const repairData = [1, 2, 0, 3, 1, 2, 4, 1, 2, 3, 1, 2];
    
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: last12Months,
            datasets: [{
                label: 'Maintenances',
                data: maintenanceData,
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                tension: 0.4
            }, {
                label: 'Réparations',
                data: repairData,
                borderColor: '#f59e0b',
                backgroundColor: 'rgba(245, 158, 11, 0.1)',
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function generateReport(type) {
    let reportData = '';
    let filename = '';
    
    if (type === 'maintenance') {
        reportData = generateMaintenanceReport();
        filename = 'rapport_maintenance_' + new Date().toISOString().split('T')[0] + '.txt';
    } else if (type === 'repairs') {
        reportData = generateRepairsReport();
        filename = 'rapport_reparations_' + new Date().toISOString().split('T')[0] + '.txt';
    }
    
    downloadReport(reportData, filename);
}

function generateMaintenanceReport() {
    let report = '=== RAPPORT DE MAINTENANCE ===\n\n';
    report += `Date de génération: ${new Date().toLocaleDateString('fr-FR')}\n\n`;
    
    report += '=== STATISTIQUES GÉNÉRALES ===\n';
    report += `Total des machines: ${machines.length}\n`;
    report += `Maintenances programmées: ${maintenanceAlerts.filter(a => !a.completed).length}\n`;
    report += `Maintenances terminées: ${maintenanceHistory.length}\n`;
    report += `Maintenances en retard: ${maintenanceAlerts.filter(a => new Date(a.date) < new Date() && !a.completed).length}\n\n`;
    
    report += '=== HISTORIQUE DES MAINTENANCES ===\n';
    maintenanceHistory.forEach(maintenance => {
        const machine = machines.find(m => m.id === maintenance.machineId);
        report += `${formatDate(maintenance.date)} - ${machine ? machine.name : 'Machine supprimée'}\n`;
        report += `  Technicien: ${maintenance.technician}\n`;
        report += `  Description: ${maintenance.description}\n\n`;
    });
    
    return report;
}

function generateRepairsReport() {
    let report = '=== RAPPORT DE RÉPARATIONS ===\n\n';
    report += `Date de génération: ${new Date().toLocaleDateString('fr-FR')}\n\n`;
    
    report += '=== STATISTIQUES GÉNÉRALES ===\n';
    report += `Total des réparations: ${repairs.length}\n`;
    report += `Réparations en cours: ${repairs.filter(r => r.status === 'in_progress').length}\n`;
    report += `Réparations terminées: ${repairs.filter(r => r.status === 'completed').length}\n`;
    report += `Coût total des réparations: ${repairs.filter(r => r.status === 'completed').reduce((sum, r) => sum + (r.actualCost || 0), 0).toFixed(2)}€\n\n`;
    
    report += '=== RÉPARATIONS PAR PRIORITÉ ===\n';
    const priorities = ['urgent', 'high', 'medium', 'low'];
    priorities.forEach(priority => {
        const count = repairs.filter(r => r.priority === priority).length;
        report += `${getPriorityText(priority)}: ${count}\n`;
    });
    
    report += '\n=== DÉTAIL DES RÉPARATIONS ===\n';
    repairs.forEach(repair => {
        const machine = machines.find(m => m.id === repair.machineId);
        report += `${repair.title} - ${machine ? machine.name : 'Machine supprimée'}\n`;
        report += `  Statut: ${getStatusText(repair.status)}\n`;
        report += `  Priorité: ${getPriorityText(repair.priority)}\n`;
        report += `  Technicien: ${repair.technician || 'Non assigné'}\n`;
        report += `  Coût: ${repair.actualCost || repair.estimatedCost}€\n`;
        report += `  Créée le: ${formatDate(repair.createdAt)}\n\n`;
    });
    
    return report;
}

function downloadReport(content, filename) {
    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    
    showNotification('Rapport téléchargé avec succès', 'success');
}

// Variables pour le calendrier
let currentCalendarDate = new Date(2025, 9, 1); // Octobre 2025
let currentCalendarView = 'month';
let selectedUserId = 'all';
let selectedEventType = 'all';

function renderCalendar() {
    const calendarGrid = document.getElementById('calendarGrid');
    const view = document.getElementById('calendarView').value;
    
    if (!calendarGrid) {
        console.error('calendarGrid not found');
        return;
    }
    
    calendarGrid.innerHTML = '';
    currentCalendarView = view;
    
    // Mettre à jour les filtres
    updateCalendarFilters();
    
    if (view === 'month') {
        renderMonthView();
    } else if (view === 'week') {
        renderWeekView();
    } else if (view === 'day') {
        renderDayView();
    }
}

function updateCalendarFilters() {
    // Mettre à jour le filtre utilisateur
    const userFilter = document.getElementById('calendarUserFilter');
    userFilter.innerHTML = '<option value="all">Tous les utilisateurs</option>';
    
    users.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.name;
        userFilter.appendChild(option);
    });
    
    // Mettre à jour le filtre machine
    const machineFilter = document.getElementById('calendarMachine');
    machineFilter.innerHTML = '<option value="">Toutes les machines</option>';
    
    machines.forEach(machine => {
        const option = document.createElement('option');
        option.value = machine.id;
        option.textContent = machine.name;
        machineFilter.appendChild(option);
    });
}

function filterCalendarByUser() {
    selectedUserId = document.getElementById('calendarUserFilter').value;
    renderCalendar();
}

function filterCalendarByEvent() {
    selectedEventType = document.getElementById('calendarEventFilter').value;
    renderCalendar();
}

function changeCalendarView() {
    renderCalendar();
}

function showAddUserScheduleModal() {
    // Remplir la liste des utilisateurs
    const userSelect = document.getElementById('scheduleUser');
    userSelect.innerHTML = '<option value="">Sélectionner un utilisateur</option>';
    
    users.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.name;
        userSelect.appendChild(option);
    });
    
    // Ouvrir le modal
    document.getElementById('addUserScheduleModal').classList.add('show');
}

function handleAddUserSchedule(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const newSchedule = {
        id: Date.now(),
        userId: parseInt(formData.get('userId')),
        date: formData.get('date'),
        type: formData.get('type'),
        description: formData.get('description'),
        isFullDay: formData.get('isFullDay') === 'on'
    };
    
    // Ajouter les heures si ce n'est pas une journée entière
    if (!newSchedule.isFullDay) {
        const startTime = formData.get('startTime');
        const endTime = formData.get('endTime');
        if (startTime) newSchedule.startTime = startTime;
        if (endTime) newSchedule.endTime = endTime;
    }
    
    userSchedules.push(newSchedule);
    saveDataToStorage();
    
    // Notification
    showNotification('Planning ajouté avec succès', 'success');
    
    // Fermer le modal et recharger le calendrier
    closeModal('addUserScheduleModal');
    renderCalendar();
    
    // Reset du formulaire
    e.target.reset();
    document.getElementById('timeFields').style.display = 'none';
}

function toggleTimeFields() {
    const scheduleType = document.getElementById('scheduleType').value;
    const timeFields = document.getElementById('timeFields');
    const fullDayCheckbox = document.getElementById('scheduleFullDay');
    
    // Types qui nécessitent des heures spécifiques
    const timeRequiredTypes = [
        'machine_arrival', 
        'maintenance_preventive', 
        'audit', 
        'equipment_test', 
        'inspection', 
        'installation', 
        'repair_scheduled',
        'meeting'
    ];
    
    if (timeRequiredTypes.includes(scheduleType)) {
        timeFields.style.display = 'block';
        fullDayCheckbox.checked = false;
    } else {
        timeFields.style.display = 'none';
    }
}

// Fonction pour obtenir tous les événements d'une date donnée
function getEventsForDate(date, userId = 'all', eventType = 'all') {
    const events = [];
    const dateStr = date.toISOString().split('T')[0];
    
    // Maintenances programmées
    if (eventType === 'all' || eventType === 'maintenance') {
        scheduledMaintenances.forEach(maintenance => {
            if (!maintenance.scheduledDate) return;
            const maintenanceDate = new Date(maintenance.scheduledDate).toISOString().split('T')[0];
            if (maintenanceDate === dateStr) {
                if (userId === 'all' || maintenance.assignedTechnician === userId) {
                    events.push({
                        type: 'maintenance',
                        title: `Maintenance: ${maintenance.machineName}`,
                        description: maintenance.description,
                        color: 'maintenance',
                        priority: maintenance.priority || 'medium'
                    });
                }
            }
        });
    }
    
    // Réparations
    if (eventType === 'all' || eventType === 'repair') {
        repairs.forEach(repair => {
            if (!repair.date) return;
            const repairDate = new Date(repair.date).toISOString().split('T')[0];
            if (repairDate === dateStr) {
                if (userId === 'all' || repair.technicianId === userId) {
                    events.push({
                        type: 'repair',
                        title: `Réparation: ${repair.machineName}`,
                        description: repair.description,
                        color: 'repair',
                        priority: repair.priority
                    });
                }
            }
        });
    }
    
    // Tickets avec dates limites
    if (eventType === 'all' || eventType === 'ticket') {
        tickets.forEach(ticket => {
            if (ticket.expectedDate) {
                const ticketDate = new Date(ticket.expectedDate).toISOString().split('T')[0];
                if (ticketDate === dateStr) {
                    if (userId === 'all' || ticket.assigneeId === userId) {
                        events.push({
                            type: 'ticket',
                            title: `Ticket: ${ticket.title}`,
                            description: `Échéance: ${ticket.ticketNumber}`,
                            color: ticket.priority === 'urgent' ? 'urgent' : 'ticket',
                            priority: ticket.priority,
                            overdue: new Date(ticket.expectedDate) < new Date()
                        });
                    }
                }
            }
        });
    }
    
    // Jours fériés
    if (eventType === 'all' || eventType === 'holiday') {
        holidays.forEach(holiday => {
            if (holiday.date === dateStr) {
                events.push({
                    type: 'holiday',
                    title: holiday.name,
                    description: 'Jour férié',
                    color: 'holiday',
                    priority: 'holiday'
                });
            }
        });
    }
    
    // Weekends
    if (eventType === 'all' || eventType === 'weekend') {
        const dayOfWeek = date.getDay();
        if (dayOfWeek === 0 || dayOfWeek === 6) { // Dimanche ou Samedi
            events.push({
                type: 'weekend',
                title: dayOfWeek === 0 ? 'Dimanche' : 'Samedi',
                description: 'Weekend',
                color: 'weekend',
                priority: 'weekend'
            });
        }
    }
    
    // Planning des utilisateurs (congés, etc.)
    if (eventType === 'all' || ['vacation', 'sick', 'training', 'meeting', 'machine_arrival', 'maintenance_preventive', 'audit', 'equipment_test', 'inspection', 'installation', 'repair_scheduled'].includes(eventType)) {
        userSchedules.forEach(schedule => {
            if (schedule.date === dateStr) {
                if (userId === 'all' || schedule.userId === userId) {
                    if (eventType === 'all' || eventType === schedule.type) {
                        const user = users.find(u => u.id === schedule.userId);
                        const typeLabels = {
                            'vacation': 'Congés',
                            'sick': 'Arrêt maladie',
                            'training': 'Formation',
                            'meeting': 'Réunion',
                            'machine_arrival': 'Arrivage Machine',
                            'maintenance_preventive': 'Maintenance Préventive',
                            'audit': 'Audit',
                            'equipment_test': 'Test Équipement',
                            'inspection': 'Inspection',
                            'installation': 'Installation',
                            'repair_scheduled': 'Réparation Programmée',
                            'other': 'Autre'
                        };
                        
                        let title = `${typeLabels[schedule.type] || 'Événement'}: ${user ? user.name : 'Utilisateur'}`;
                        
                        // Ajouter les heures si disponibles
                        if (schedule.startTime && schedule.endTime) {
                            title += ` (${schedule.startTime}-${schedule.endTime})`;
                        }
                        
                        // Couleurs par utilisateur
                        let userColor = '#6b7280'; // Couleur par défaut
                        switch(schedule.userId) {
                            case 1: userColor = '#3b82f6'; break; // Patrice - Bleu
                            case 2: userColor = '#10b981'; break; // David - Vert
                            case 3: userColor = '#8b5cf6'; break; // Sophie - Violet
                            case 4: userColor = '#f59e0b'; break; // Thomas - Orange
                        }
                        
                        events.push({
                            type: schedule.type,
                            title: title,
                            description: schedule.description,
                            color: schedule.type,
                            userColor: userColor,
                            userId: schedule.userId,
                            priority: schedule.type,
                            startTime: schedule.startTime,
                            endTime: schedule.endTime
                        });
                    }
                }
            }
        });
    }
    
    return events;
}

function renderMonthView() {
    const calendarGrid = document.getElementById('calendarGrid');
    const year = currentCalendarDate.getFullYear();
    const month = currentCalendarDate.getMonth();
    
    // Mettre à jour le titre
    const titleElement = document.getElementById('calendarTitle');
    if (titleElement) {
        titleElement.textContent = currentCalendarDate.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    }
    
    // En-têtes des jours
    const dayHeaders = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];
    dayHeaders.forEach(day => {
        const header = document.createElement('div');
        header.className = 'calendar-day-header';
        header.textContent = day;
        calendarGrid.appendChild(header);
    });
    
    // Premier jour du mois
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    // Générer les jours
    for (let i = 0; i < 42; i++) {
        const date = new Date(startDate);
        date.setDate(startDate.getDate() + i);
        
        const dayElement = document.createElement('div');
        dayElement.className = 'calendar-day';
        
        const dayOfWeek = date.getDay();
        const dateStr = date.toISOString().split('T')[0];
        
        if (date.getMonth() !== month) {
            dayElement.classList.add('other-month');
        }
        
        if (date.toDateString() === new Date().toDateString()) {
            dayElement.classList.add('today');
        }
        
        // Vérifier si c'est un jour férié
        const isHoliday = holidays.some(holiday => holiday.date === dateStr);
        if (isHoliday) {
            dayElement.classList.add('holiday');
        }
        
        // Vérifier si c'est un weekend (samedi ou dimanche)
        if (dayOfWeek === 0 || dayOfWeek === 6) {
            dayElement.classList.add('weekend');
        }
        
        // Vérifier le planning alternant pour David
        if (selectedUserId === '2' || selectedUserId === 'all') {
            const david = users.find(u => u.id === 2);
            if (david && david.employeeType === 'alternant') {
                const weekNumber = getWeekNumber(date);
                const isWorkingDay = (weekNumber % 2 === 1 && [1, 2, 3].includes(dayOfWeek)) || 
                                   (weekNumber % 2 === 0 && [4, 5].includes(dayOfWeek));
                
                if (isWorkingDay && !isHoliday && dayOfWeek !== 0 && dayOfWeek !== 6) {
                    dayElement.classList.add('alternant-work');
                } else if (!isHoliday && dayOfWeek !== 0 && dayOfWeek !== 6) {
                    dayElement.classList.add('alternant-off');
                }
            }
        }
        
        dayElement.innerHTML = `<div class="calendar-day-number">${date.getDate()}</div>`;
        
        // Ajouter les événements
        const events = getEventsForDate(date, selectedUserId, selectedEventType);
        events.forEach(event => {
            const eventElement = document.createElement('div');
            eventElement.className = `calendar-event ${event.color}`;
            
            // Appliquer la couleur de l'utilisateur si disponible
            if (event.userColor) {
                eventElement.style.backgroundColor = event.userColor;
                eventElement.style.color = 'white';
                eventElement.style.borderLeft = `4px solid ${event.userColor}`;
            }
            
            eventElement.innerHTML = `
                <div class="event-title">${event.title}</div>
                ${event.description ? `<div class="event-description">${event.description}</div>` : ''}
            `;
            
            // Ajouter un style spécial pour les événements en retard
            if (event.overdue) {
                eventElement.style.borderLeft = '3px solid #ef4444';
                eventElement.style.backgroundColor = '#fef2f2';
            }
            
            dayElement.appendChild(eventElement);
        });
        
        calendarGrid.appendChild(dayElement);
    }
}

// Fonction pour calculer le numéro de semaine
function getWeekNumber(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
}

function renderWeekView() {
    const calendarGrid = document.getElementById('calendarGrid');
    calendarGrid.innerHTML = '<div class="calendar-message">Vue semaine en développement</div>';
}

function renderDayView() {
    const calendarGrid = document.getElementById('calendarGrid');
    calendarGrid.innerHTML = '<div class="calendar-message">Vue jour en développement</div>';
}


function navigateCalendar(direction) {
    const view = document.getElementById('calendarView').value;
    
    if (view === 'month') {
        if (direction === 'prev') {
            currentCalendarDate.setMonth(currentCalendarDate.getMonth() - 1);
        } else {
            currentCalendarDate.setMonth(currentCalendarDate.getMonth() + 1);
        }
    } else if (view === 'week') {
        if (direction === 'prev') {
            currentCalendarDate.setDate(currentCalendarDate.getDate() - 7);
        } else {
            currentCalendarDate.setDate(currentCalendarDate.getDate() + 7);
        }
    } else if (view === 'day') {
        if (direction === 'prev') {
            currentCalendarDate.setDate(currentCalendarDate.getDate() - 1);
        } else {
            currentCalendarDate.setDate(currentCalendarDate.getDate() + 1);
        }
    }
    
    renderCalendar();
}

function showAddMaintenanceModal() {
    const maintenanceMachineSelect = document.getElementById('maintenanceMachine');
    const maintenanceTechnicianSelect = document.getElementById('maintenanceTechnician');
    
    // Remplir les machines
    maintenanceMachineSelect.innerHTML = '<option value="">Sélectionner une machine</option>';
    machines.forEach(machine => {
        const option = document.createElement('option');
        option.value = machine.id;
        option.textContent = `${machine.name} (${machine.location})`;
        maintenanceMachineSelect.appendChild(option);
    });
    
    // Remplir les techniciens
    maintenanceTechnicianSelect.innerHTML = '<option value="">Sélectionner un technicien</option>';
    technicians.forEach(technician => {
        const option = document.createElement('option');
        option.value = technician.id;
        option.textContent = technician.name;
        maintenanceTechnicianSelect.appendChild(option);
    });
    
    document.getElementById('addMaintenanceModal').classList.add('show');
}

function handleAddMaintenance(e) {
    e.preventDefault();
    
    const maintenance = {
        id: Date.now(),
        machineId: parseInt(document.getElementById('maintenanceMachine').value),
        technicianId: parseInt(document.getElementById('maintenanceTechnician').value) || null,
        date: document.getElementById('maintenanceDate').value,
        time: document.getElementById('maintenanceTime').value,
        type: document.getElementById('maintenanceType').value,
        description: document.getElementById('maintenanceDescription').value,
        duration: parseFloat(document.getElementById('maintenanceDuration').value) || 0,
        createdAt: new Date()
    };
    
    scheduledMaintenances.push(maintenance);
    saveDataToStorage();
    renderCalendar();
    closeModal('addMaintenanceModal');
    document.getElementById('addMaintenanceForm').reset();
    
    showNotification('Maintenance programmée avec succès', 'success');
}

// Techniciens
function renderTechnicians() {
    const techniciansList = document.getElementById('techniciansList');
    
    techniciansList.innerHTML = technicians.map(technician => `
        <div class="technician-item">
            <div class="technician-info">
                <h3>${technician.name}</h3>
                <p>${getSpecialtyText(technician.specialty)} - ${getLevelText(technician.level)}</p>
                <p>${technician.email} - ${technician.phone}</p>
            </div>
            <div class="technician-status ${technician.status}">
                ${getStatusText(technician.status)}
            </div>
        </div>
    `).join('');
    
    renderWorkloadChart();
}

function renderWorkloadChart() {
    const ctx = document.getElementById('workloadCanvas').getContext('2d');
    
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: technicians.map(t => t.name),
            datasets: [{
                data: technicians.map(t => t.workload),
                backgroundColor: [
                    '#10b981',
                    '#f59e0b',
                    '#ef4444',
                    '#8b5cf6',
                    '#06b6d4'
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

function showAddTechnicianModal() {
    document.getElementById('addTechnicianModal').classList.add('show');
}

function handleAddTechnician(e) {
    e.preventDefault();
    
    const technician = {
        id: Date.now(),
        name: document.getElementById('technicianName').value,
        email: document.getElementById('technicianEmail').value,
        phone: document.getElementById('technicianPhone').value,
        specialty: document.getElementById('technicianSpecialty').value,
        level: document.getElementById('technicianLevel').value,
        availability: document.getElementById('technicianAvailability').value,
        status: 'available',
        workload: 0,
        createdAt: new Date()
    };
    
    technicians.push(technician);
    saveDataToStorage();
    renderTechnicians();
    closeModal('addTechnicianModal');
    document.getElementById('addTechnicianForm').reset();
    
    showNotification('Technicien ajouté avec succès', 'success');
}

// Stock
function renderInventory() {
    const inventoryList = document.getElementById('inventoryList');
    const inventoryAlerts = document.getElementById('inventoryAlerts');
    
    inventoryList.innerHTML = inventory.map(item => {
        const stockClass = item.status === 'low_stock' ? 'low-stock' : 
                          item.status === 'out_of_stock' ? 'out-of-stock' : '';
        
        return `
            <div class="inventory-item ${stockClass}">
                <div class="inventory-info">
                    <h3>${item.name}</h3>
                    <p>${item.partNumber} - ${getCategoryText(item.category)}</p>
                    <p>Fournisseur: ${item.supplier} - ${item.location}</p>
                </div>
                <div class="inventory-stock">
                    <div class="inventory-quantity">${item.quantity}</div>
                    <div class="inventory-status ${item.status}">${getStockStatusText(item.status)}</div>
                    <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 0.25rem;">
                        Min: ${item.minStock} - Prix: ${item.price}€
                    </div>
                </div>
            </div>
        `;
    }).join('');
    
    // Alertes de stock
    const lowStockItems = inventory.filter(item => item.status === 'low_stock' || item.status === 'out_of_stock');
    inventoryAlerts.innerHTML = lowStockItems.map(item => `
        <div class="alert-item ${item.status === 'out_of_stock' ? 'overdue' : 'scheduled'}">
            <div class="alert-header">
                <span class="alert-machine">${item.name}</span>
                <span class="alert-date">${item.partNumber}</span>
            </div>
            <div class="alert-description">
                Stock: ${item.quantity} (minimum: ${item.minStock})
            </div>
            <div class="alert-actions">
                <button class="btn btn-sm btn-primary" onclick="updateStock(${item.id})">
                    <i class="fas fa-plus"></i> Réapprovisionner
                </button>
            </div>
        </div>
    `).join('');
}

function showAddInventoryModal() {
    document.getElementById('addInventoryModal').classList.add('show');
}

function handleAddInventory(e) {
    e.preventDefault();
    
    const item = {
        id: Date.now(),
        name: document.getElementById('inventoryName').value,
        partNumber: document.getElementById('inventoryPartNumber').value,
        category: document.getElementById('inventoryCategory').value,
        quantity: parseInt(document.getElementById('inventoryQuantity').value),
        minStock: parseInt(document.getElementById('inventoryMinStock').value),
        price: parseFloat(document.getElementById('inventoryPrice').value) || 0,
        supplier: document.getElementById('inventorySupplier').value,
        location: document.getElementById('inventoryLocation').value,
        status: getStockStatus(parseInt(document.getElementById('inventoryQuantity').value), parseInt(document.getElementById('inventoryMinStock').value)),
        createdAt: new Date()
    };
    
    inventory.push(item);
    saveDataToStorage();
    renderInventory();
    closeModal('addInventoryModal');
    document.getElementById('addInventoryForm').reset();
    
    showNotification('Pièce ajoutée au stock avec succès', 'success');
}

function updateStock(itemId) {
    const item = inventory.find(i => i.id === itemId);
    if (!item) return;
    
    const newQuantity = prompt(`Nouvelle quantité pour ${item.name}:`, item.quantity);
    if (newQuantity !== null && !isNaN(newQuantity)) {
        item.quantity = parseInt(newQuantity);
        item.status = getStockStatus(item.quantity, item.minStock);
        saveDataToStorage();
        renderInventory();
        showNotification('Stock mis à jour', 'success');
    }
}

function getStockStatus(quantity, minStock) {
    if (quantity === 0) return 'out_of_stock';
    if (quantity <= minStock) return 'low_stock';
    return 'in_stock';
}

function filterInventory() {
    const searchTerm = document.getElementById('inventorySearch').value.toLowerCase();
    const categoryFilter = document.getElementById('inventoryCategory').value;
    const statusFilter = document.getElementById('inventoryStatus').value;
    
    const filteredInventory = inventory.filter(item => {
        const matchesSearch = item.name.toLowerCase().includes(searchTerm) ||
                            item.partNumber.toLowerCase().includes(searchTerm) ||
                            item.supplier.toLowerCase().includes(searchTerm);
        const matchesCategory = !categoryFilter || item.category === categoryFilter;
        const matchesStatus = !statusFilter || item.status === statusFilter;
        
        return matchesSearch && matchesCategory && matchesStatus;
    });
    
    const inventoryList = document.getElementById('inventoryList');
    inventoryList.innerHTML = filteredInventory.map(item => {
        const stockClass = item.status === 'low_stock' ? 'low-stock' : 
                          item.status === 'out_of_stock' ? 'out-of-stock' : '';
        
        return `
            <div class="inventory-item ${stockClass}">
                <div class="inventory-info">
                    <h3>${item.name}</h3>
                    <p>${item.partNumber} - ${getCategoryText(item.category)}</p>
                    <p>Fournisseur: ${item.supplier} - ${item.location}</p>
                </div>
                <div class="inventory-stock">
                    <div class="inventory-quantity">${item.quantity}</div>
                    <div class="inventory-status ${item.status}">${getStockStatusText(item.status)}</div>
                    <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 0.25rem;">
                        Min: ${item.minStock} - Prix: ${item.price}€
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// Fonctions utilitaires pour les nouvelles fonctionnalités
function getSpecialtyText(specialty) {
    const specialties = {
        electrical: 'Électrique',
        mechanical: 'Mécanique',
        hydraulic: 'Hydraulique',
        pneumatic: 'Pneumatique',
        general: 'Généraliste'
    };
    return specialties[specialty] || specialty;
}

function getLevelText(level) {
    const levels = {
        junior: 'Junior',
        senior: 'Senior',
        expert: 'Expert'
    };
    return levels[level] || level;
}

function getCategoryText(category) {
    const categories = {
        electrical: 'Électrique',
        mechanical: 'Mécanique',
        hydraulic: 'Hydraulique',
        pneumatic: 'Pneumatique',
        consumable: 'Consommable'
    };
    return categories[category] || category;
}

function getStockStatusText(status) {
    const statuses = {
        in_stock: 'En stock',
        low_stock: 'Stock faible',
        out_of_stock: 'Rupture'
    };
    return statuses[status] || status;
}

// Gestion des Tickets
function renderTickets() {
    const activeContainer = document.getElementById('activeTickets');
    const historyContainer = document.getElementById('ticketHistory');
    
    // Tickets actifs (non fermés)
    const activeTickets = tickets.filter(ticket => 
        ticket.status !== 'closed' && ticket.status !== 'resolved'
    );
    
    activeContainer.innerHTML = activeTickets.map(ticket => {
        const machine = machines.find(m => m.id === ticket.machineId);
        const assignee = users.find(u => u.id === ticket.assigneeId);
        const priorityClass = ticket.priority === 'urgent' ? 'urgent' : '';
        
        return `
            <div class="ticket-item ${ticket.status} ${priorityClass}" onclick="showTicketDetails(${ticket.id})">
                <div class="ticket-header">
                    <div>
                        <div class="ticket-title">${ticket.title}</div>
                        <div class="ticket-id">${ticket.ticketNumber}</div>
                    </div>
                    <span class="ticket-priority ${ticket.priority}">${getPriorityText(ticket.priority)}</span>
                </div>
                <div class="ticket-description">${ticket.description}</div>
                <div class="ticket-meta">
                    <span><i class="fas fa-cogs"></i> ${machine ? machine.name : 'Aucune machine'}</span>
                    <span><i class="fas fa-user"></i> ${assignee ? assignee.name : 'Non assigné'}</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(ticket.createdAt)}</span>
                </div>
                <div class="ticket-actions">
                    <span class="ticket-status-badge ${ticket.status}">${getTicketStatusText(ticket.status)}</span>
                </div>
            </div>
        `;
    }).join('');
    
    // Historique des tickets (fermés et résolus)
    const closedTickets = tickets.filter(ticket => 
        ticket.status === 'closed' || ticket.status === 'resolved'
    );
    
    historyContainer.innerHTML = closedTickets.map(ticket => {
        const machine = machines.find(m => m.id === ticket.machineId);
        const assignee = users.find(u => u.id === ticket.assigneeId);
        
        return `
            <div class="ticket-item ${ticket.status}" onclick="showTicketDetails(${ticket.id})">
                <div class="ticket-header">
                    <div>
                        <div class="ticket-title">${ticket.title}</div>
                        <div class="ticket-id">${ticket.ticketNumber}</div>
                    </div>
                    <span class="ticket-priority ${ticket.priority}">${getPriorityText(ticket.priority)}</span>
                </div>
                <div class="ticket-description">${ticket.description}</div>
                <div class="ticket-meta">
                    <span><i class="fas fa-cogs"></i> ${machine ? machine.name : 'Aucune machine'}</span>
                    <span><i class="fas fa-user"></i> ${assignee ? assignee.name : 'Non assigné'}</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(ticket.resolvedAt || ticket.closedAt)}</span>
                </div>
                <div class="ticket-actions">
                    <span class="ticket-status-badge ${ticket.status}">${getTicketStatusText(ticket.status)}</span>
                </div>
            </div>
        `;
    }).join('');
    
    updateAssigneeFilter();
}

function showAddTicketModal() {
    const ticketMachineSelect = document.getElementById('ticketMachine');
    const ticketAssigneeSelect = document.getElementById('ticketAssignee');
    
    // Remplir les machines
    ticketMachineSelect.innerHTML = '<option value="">Sélectionner une machine (optionnel)</option>';
    machines.forEach(machine => {
        const option = document.createElement('option');
        option.value = machine.id;
        option.textContent = `${machine.name} (${machine.location})`;
        ticketMachineSelect.appendChild(option);
    });
    
    // Remplir les utilisateurs assignables
    ticketAssigneeSelect.innerHTML = '<option value="">Non assigné</option>';
    users.filter(user => user.role === 'technician' || user.role === 'manager').forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.name;
        ticketAssigneeSelect.appendChild(option);
    });
    
    document.getElementById('addTicketModal').classList.add('show');
}

function handleAddTicket(e) {
    e.preventDefault();
    
    const ticketNumber = 'TKT-' + String(tickets.length + 1).padStart(3, '0');
    
    const ticket = {
        id: Date.now(),
        ticketNumber: ticketNumber,
        title: document.getElementById('ticketTitle').value,
        description: document.getElementById('ticketDescription').value,
        priority: document.getElementById('ticketPriority').value,
        status: 'open',
        category: document.getElementById('ticketCategory').value,
        machineId: parseInt(document.getElementById('ticketMachine').value) || null,
        assigneeId: parseInt(document.getElementById('ticketAssignee').value) || null,
        createdBy: currentUser.id,
        createdAt: new Date(),
        updatedAt: new Date(),
        expectedDate: document.getElementById('ticketExpectedDate').value ? new Date(document.getElementById('ticketExpectedDate').value) : null,
        resolvedAt: null,
        closedAt: null,
        comments: []
    };
    
    tickets.push(ticket);
    saveDataToStorage();
    
    // Envoyer une notification
    sendTicketNotification(ticket, 'created');
    
    renderTickets();
    updateDashboard();
    closeModal('addTicketModal');
    document.getElementById('addTicketForm').reset();
    
    showNotification('Ticket créé avec succès', 'success');
}

function showTicketDetails(ticketId) {
    const ticket = tickets.find(t => t.id === ticketId);
    if (!ticket) return;
    
    const machine = machines.find(m => m.id === ticket.machineId);
    const assignee = users.find(u => u.id === ticket.assigneeId);
    const creator = users.find(u => u.id === ticket.createdBy);
    
    const content = document.getElementById('ticketDetailsContent');
    
    content.innerHTML = `
        <div class="ticket-details">
            <div class="detail-section">
                <h3>Informations générales</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>Numéro:</label>
                        <span>${ticket.ticketNumber}</span>
                    </div>
                    <div class="detail-item">
                        <label>Titre:</label>
                        <span>${ticket.title}</span>
                    </div>
                    <div class="detail-item">
                        <label>Statut:</label>
                        <span class="ticket-status-badge ${ticket.status}">${getTicketStatusText(ticket.status)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Priorité:</label>
                        <span class="ticket-priority ${ticket.priority}">${getPriorityText(ticket.priority)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Catégorie:</label>
                        <span>${getCategoryText(ticket.category)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Machine:</label>
                        <span>${machine ? machine.name : 'Aucune machine'}</span>
                    </div>
                </div>
            </div>
            
            <div class="detail-section">
                <h3>Description</h3>
                <p>${ticket.description}</p>
            </div>
            
            <div class="detail-section">
                <h3>Assignation</h3>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>Assigné à:</label>
                        <span>${assignee ? assignee.name : 'Non assigné'}</span>
                    </div>
                    <div class="detail-item">
                        <label>Créé par:</label>
                        <span>${creator ? creator.name : 'Utilisateur inconnu'}</span>
                    </div>
                    <div class="detail-item">
                        <label>Date de création:</label>
                        <span>${formatDate(ticket.createdAt)}</span>
                    </div>
                    <div class="detail-item">
                        <label>Dernière mise à jour:</label>
                        <span>${formatDate(ticket.updatedAt)}</span>
                    </div>
                    ${ticket.expectedDate ? `
                        <div class="detail-item">
                            <label>Date souhaitée:</label>
                            <span>${formatDate(ticket.expectedDate)}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
            
            ${renderProgressTracker(ticket)}
            
            <div class="detail-section">
                <h3>Actions</h3>
                <div class="ticket-actions">
                    ${ticket.status === 'open' ? `
                        <button class="btn btn-primary" onclick="updateTicketStatus(${ticket.id}, 'in_progress')">
                            <i class="fas fa-play"></i> Démarrer
                        </button>
                    ` : ''}
                    ${ticket.status === 'in_progress' ? `
                        <button class="btn btn-success" onclick="updateTicketStatus(${ticket.id}, 'resolved')">
                            <i class="fas fa-check"></i> Résoudre
                        </button>
                        <button class="btn btn-warning" onclick="updateTicketStatus(${ticket.id}, 'pending')">
                            <i class="fas fa-pause"></i> Mettre en attente
                        </button>
                    ` : ''}
                    ${ticket.status === 'resolved' ? `
                        <button class="btn btn-primary" onclick="updateTicketStatus(${ticket.id}, 'closed')">
                            <i class="fas fa-times"></i> Fermer
                        </button>
                    ` : ''}
                    ${ticket.status === 'pending' ? `
                        <button class="btn btn-primary" onclick="updateTicketStatus(${ticket.id}, 'in_progress')">
                            <i class="fas fa-play"></i> Reprendre
                        </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
    
    document.getElementById('ticketDetailsModal').classList.add('show');
}

function renderProgressTracker(ticket) {
    const steps = [
        { key: 'open', label: 'Ouvert', icon: 'fas fa-plus' },
        { key: 'in_progress', label: 'En cours', icon: 'fas fa-play' },
        { key: 'resolved', label: 'Résolu', icon: 'fas fa-check' },
        { key: 'closed', label: 'Fermé', icon: 'fas fa-times' }
    ];
    
    const currentStepIndex = steps.findIndex(step => step.key === ticket.status);
    
    return `
        <div class="detail-section">
            <h3>Suivi d'avancement</h3>
            <div class="progress-tracker">
                <div class="progress-steps">
                    ${steps.map((step, index) => {
                        let stepClass = '';
                        if (index < currentStepIndex) {
                            stepClass = 'completed';
                        } else if (index === currentStepIndex) {
                            stepClass = 'current';
                        }
                        
                        return `
                            <div class="progress-step ${stepClass}">
                                <div class="progress-step-icon">
                                    <i class="${step.icon}"></i>
                                </div>
                                <div class="progress-step-label">${step.label}</div>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        </div>
    `;
}

function updateTicketStatus(ticketId, newStatus) {
    const ticketIndex = tickets.findIndex(t => t.id === ticketId);
    if (ticketIndex === -1) return;
    
    const ticket = tickets[ticketIndex];
    ticket.status = newStatus;
    ticket.updatedAt = new Date();
    
    if (newStatus === 'resolved') {
        ticket.resolvedAt = new Date();
        sendTicketNotification(ticket, 'resolved');
    } else if (newStatus === 'closed') {
        ticket.closedAt = new Date();
    } else {
        sendTicketNotification(ticket, 'updated');
    }
    
    saveDataToStorage();
    renderTickets();
    updateDashboard();
    showNotification(`Ticket ${getTicketStatusText(newStatus)}`, 'success');
}

function updateAssigneeFilter() {
    const assigneeFilter = document.getElementById('ticketAssigneeFilter');
    assigneeFilter.innerHTML = '<option value="">Tous les assignés</option>';
    
    users.filter(user => user.role === 'technician' || user.role === 'manager').forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = user.name;
        assigneeFilter.appendChild(option);
    });
}

function filterTickets() {
    const searchTerm = document.getElementById('ticketSearch').value.toLowerCase();
    const statusFilter = document.getElementById('ticketStatusFilter').value;
    const priorityFilter = document.getElementById('ticketPriorityFilter').value;
    const assigneeFilter = document.getElementById('ticketAssigneeFilter').value;
    
    const filteredTickets = tickets.filter(ticket => {
        const machine = machines.find(m => m.id === ticket.machineId);
        const assignee = users.find(u => u.id === ticket.assigneeId);
        
        const matchesSearch = ticket.title.toLowerCase().includes(searchTerm) ||
                            ticket.description.toLowerCase().includes(searchTerm) ||
                            ticket.ticketNumber.toLowerCase().includes(searchTerm) ||
                            (machine && machine.name.toLowerCase().includes(searchTerm));
        const matchesStatus = !statusFilter || ticket.status === statusFilter;
        const matchesPriority = !priorityFilter || ticket.priority === priorityFilter;
        const matchesAssignee = !assigneeFilter || ticket.assigneeId == assigneeFilter;
        
        return matchesSearch && matchesStatus && matchesPriority && matchesAssignee;
    });
    
    renderFilteredTickets(filteredTickets);
}

function renderFilteredTickets(filteredTickets) {
    const activeContainer = document.getElementById('activeTickets');
    const historyContainer = document.getElementById('ticketHistory');
    
    const activeTickets = filteredTickets.filter(ticket => 
        ticket.status !== 'closed' && ticket.status !== 'resolved'
    );
    
    const closedTickets = filteredTickets.filter(ticket => 
        ticket.status === 'closed' || ticket.status === 'resolved'
    );
    
    activeContainer.innerHTML = activeTickets.map(ticket => {
        const machine = machines.find(m => m.id === ticket.machineId);
        const assignee = users.find(u => u.id === ticket.assigneeId);
        const priorityClass = ticket.priority === 'urgent' ? 'urgent' : '';
        
        return `
            <div class="ticket-item ${ticket.status} ${priorityClass}" onclick="showTicketDetails(${ticket.id})">
                <div class="ticket-header">
                    <div>
                        <div class="ticket-title">${ticket.title}</div>
                        <div class="ticket-id">${ticket.ticketNumber}</div>
                    </div>
                    <span class="ticket-priority ${ticket.priority}">${getPriorityText(ticket.priority)}</span>
                </div>
                <div class="ticket-description">${ticket.description}</div>
                <div class="ticket-meta">
                    <span><i class="fas fa-cogs"></i> ${machine ? machine.name : 'Aucune machine'}</span>
                    <span><i class="fas fa-user"></i> ${assignee ? assignee.name : 'Non assigné'}</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(ticket.createdAt)}</span>
                </div>
                <div class="ticket-actions">
                    <span class="ticket-status-badge ${ticket.status}">${getTicketStatusText(ticket.status)}</span>
                </div>
            </div>
        `;
    }).join('');
    
    historyContainer.innerHTML = closedTickets.map(ticket => {
        const machine = machines.find(m => m.id === ticket.machineId);
        const assignee = users.find(u => u.id === ticket.assigneeId);
        
        return `
            <div class="ticket-item ${ticket.status}" onclick="showTicketDetails(${ticket.id})">
                <div class="ticket-header">
                    <div>
                        <div class="ticket-title">${ticket.title}</div>
                        <div class="ticket-id">${ticket.ticketNumber}</div>
                    </div>
                    <span class="ticket-priority ${ticket.priority}">${getPriorityText(ticket.priority)}</span>
                </div>
                <div class="ticket-description">${ticket.description}</div>
                <div class="ticket-meta">
                    <span><i class="fas fa-cogs"></i> ${machine ? machine.name : 'Aucune machine'}</span>
                    <span><i class="fas fa-user"></i> ${assignee ? assignee.name : 'Non assigné'}</span>
                    <span><i class="fas fa-calendar"></i> ${formatDate(ticket.resolvedAt || ticket.closedAt)}</span>
                </div>
                <div class="ticket-actions">
                    <span class="ticket-status-badge ${ticket.status}">${getTicketStatusText(ticket.status)}</span>
                </div>
            </div>
        `;
    }).join('');
}

// Gestion des Utilisateurs - Firebase Realtime Database
let usersFirebase = []; // Utilisateurs depuis Firebase

// Charger les utilisateurs depuis Firebase
async function loadUsersFromFirebase() {
    try {
        const response = await fetch('/api/users/list');
        const data = await response.json();
        
        if (data.success) {
            usersFirebase = data.users || [];
            renderUsers();
            return usersFirebase;
        } else {
            if (response.status === 403) {
                showNotification('Accès refusé. Seul le superadmin peut accéder à cette page.', 'error');
                // Masquer les boutons d'action si l'utilisateur n'est pas superadmin
                const addButton = document.querySelector('#users .btn-primary');
                if (addButton) addButton.style.display = 'none';
            } else {
                showNotification('Erreur lors du chargement des utilisateurs: ' + (data.error || 'Erreur inconnue'), 'error');
            }
            return [];
        }
    } catch (error) {
        console.error('Erreur lors du chargement des utilisateurs:', error);
        showNotification('Erreur lors du chargement des utilisateurs', 'error');
        return [];
    }
}

// Rendre les utilisateurs dans la liste
function renderUsers() {
    const usersList = document.getElementById('usersList');
    const userStats = document.getElementById('userStats');
    
    if (!usersList) return;
    
    // Vérifier si l'utilisateur est superadmin
    const isSuperAdmin = checkIfSuperAdmin();
    
    usersList.innerHTML = usersFirebase.map(user => {
        const nom = user.nom || 'Sans nom';
        const email = user.email || '';
        const telephone = user.telephone || '';
        const role = user.role || 'utilisateur';
        const statut = user.statut || 'actif';
        
        const actions = isSuperAdmin ? `
            <div style="display: flex; gap: 0.5rem; margin-top: 0.5rem;">
                <button class="btn btn-sm btn-primary" onclick="editUser('${user.userId}')">
                    <i class="fas fa-edit"></i> Modifier
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteUser('${user.userId}')">
                    <i class="fas fa-trash"></i> Supprimer
                </button>
            </div>
        ` : '';
        
        return `
        <div class="user-item">
            <div class="user-info">
                <h3>${nom}</h3>
                <p>${email}</p>
                <p>${telephone || 'Non renseigné'}</p>
                ${actions}
            </div>
            <div style="display: flex; align-items: center; gap: 1rem; flex-direction: column;">
                <span class="user-role ${role}">${getRoleText(role)}</span>
                <span class="user-status ${statut}">${statut === 'actif' ? 'Actif' : 'Inactif'}</span>
            </div>
        </div>
    `;
    }).join('');
    
    // Statistiques par rôle
    const roleStats = {};
    usersFirebase.forEach(user => {
        const role = user.role || 'utilisateur';
        roleStats[role] = (roleStats[role] || 0) + 1;
    });
    
    if (userStats) {
    userStats.innerHTML = Object.entries(roleStats).map(([role, count]) => `
        <div class="stat-item">
            <div>
                <h4>${getRoleText(role)}</h4>
                <p>Utilisateurs</p>
            </div>
            <div class="count">${count}</div>
        </div>
    `).join('');
}
}

// Vérifier si l'utilisateur est superadmin
async function checkIfSuperAdmin() {
    try {
        // Essayer de récupérer depuis localStorage (mis à jour lors de la connexion)
        const userRole = localStorage.getItem('userRole');
        if (userRole === 'superadmin') {
            return true;
        }
        
        // Sinon, faire un appel API pour vérifier
        const response = await fetch('/api/users/check-role');
        const data = await response.json();
        if (data.success && data.role === 'superadmin') {
            localStorage.setItem('userRole', 'superadmin');
            return true;
        }
        return false;
    } catch (error) {
        console.error('Erreur lors de la vérification du rôle:', error);
        return false;
    }
}

// Afficher le modal d'ajout d'utilisateur
function showAddUserModal() {
    if (!checkIfSuperAdmin()) {
        showNotification('Accès refusé. Seul le superadmin peut créer des utilisateurs.', 'error');
        return;
    }
    document.getElementById('addUserModal').classList.add('show');
}

// Créer un nouvel utilisateur
async function handleAddUser(e) {
    e.preventDefault();
    
    if (!checkIfSuperAdmin()) {
        showNotification('Accès refusé. Seul le superadmin peut créer des utilisateurs.', 'error');
        return;
    }
    
    const userData = {
        nom: document.getElementById('userName').value,
        email: document.getElementById('userEmail').value,
        nomUtilisateur: document.getElementById('userUsername').value,
        password: document.getElementById('userPassword').value,
        role: document.getElementById('userRole').value,
        telephone: document.getElementById('userPhone').value,
        statut: document.getElementById('userStatus')?.value || 'actif'
    };
    
    // Validation
    if (!userData.email || !userData.password) {
        showNotification('Email et mot de passe sont requis', 'error');
        return;
    }
    
    if (userData.password.length < 6) {
        showNotification('Le mot de passe doit contenir au moins 6 caractères', 'error');
        return;
    }
    
    try {
        const response = await fetch('/api/users/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Utilisateur créé avec succès', 'success');
    closeModal('addUserModal');
    document.getElementById('addUserForm').reset();
            await loadUsersFromFirebase(); // Recharger la liste
        } else {
            showNotification('Erreur: ' + (data.error || 'Erreur lors de la création'), 'error');
        }
    } catch (error) {
        console.error('Erreur:', error);
        showNotification('Erreur lors de la création de l\'utilisateur', 'error');
    }
}

// Modifier un utilisateur
async function editUser(userId) {
    if (!checkIfSuperAdmin()) {
        showNotification('Accès refusé. Seul le superadmin peut modifier des utilisateurs.', 'error');
        return;
    }
    
    try {
        const response = await fetch(`/api/users/${userId}`);
        const data = await response.json();
        
        if (data.success) {
            const user = data.user;
            // Remplir le formulaire de modification
            document.getElementById('editUserId').value = user.userId;
            document.getElementById('editUserName').value = user.nom || '';
            document.getElementById('editUserEmail').value = user.email || '';
            document.getElementById('editUserUsername').value = user.nomUtilisateur || '';
            document.getElementById('editUserRole').value = user.role || 'utilisateur';
            document.getElementById('editUserPhone').value = user.telephone || '';
            document.getElementById('editUserStatus').value = user.statut || 'actif';
            
            document.getElementById('editUserModal').classList.add('show');
        } else {
            showNotification('Erreur: ' + (data.error || 'Utilisateur non trouvé'), 'error');
        }
    } catch (error) {
        console.error('Erreur:', error);
        showNotification('Erreur lors du chargement de l\'utilisateur', 'error');
    }
}

// Enregistrer les modifications d'un utilisateur
async function handleUpdateUser(e) {
    e.preventDefault();
    
    if (!checkIfSuperAdmin()) {
        showNotification('Accès refusé. Seul le superadmin peut modifier des utilisateurs.', 'error');
        return;
    }
    
    const userId = document.getElementById('editUserId').value;
    const userData = {
        nom: document.getElementById('editUserName').value,
        email: document.getElementById('editUserEmail').value,
        nomUtilisateur: document.getElementById('editUserUsername').value,
        role: document.getElementById('editUserRole').value,
        telephone: document.getElementById('editUserPhone').value,
        statut: document.getElementById('editUserStatus').value
    };
    
    // Si un nouveau mot de passe est fourni
    const newPassword = document.getElementById('editUserPassword').value;
    if (newPassword && newPassword.length > 0) {
        if (newPassword.length < 6) {
            showNotification('Le mot de passe doit contenir au moins 6 caractères', 'error');
            return;
        }
        userData.password = newPassword;
    }
    
    try {
        const response = await fetch(`/api/users/${userId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Utilisateur mis à jour avec succès', 'success');
            closeModal('editUserModal');
            document.getElementById('editUserForm').reset();
            await loadUsersFromFirebase(); // Recharger la liste
        } else {
            showNotification('Erreur: ' + (data.error || 'Erreur lors de la mise à jour'), 'error');
        }
    } catch (error) {
        console.error('Erreur:', error);
        showNotification('Erreur lors de la mise à jour de l\'utilisateur', 'error');
    }
}

// Supprimer un utilisateur
async function deleteUser(userId) {
    if (!checkIfSuperAdmin()) {
        showNotification('Accès refusé. Seul le superadmin peut supprimer des utilisateurs.', 'error');
        return;
    }
    
    if (!confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ? Cette action est irréversible.')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/users/${userId}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (data.success) {
            showNotification('Utilisateur supprimé avec succès', 'success');
            await loadUsersFromFirebase(); // Recharger la liste
        } else {
            showNotification('Erreur: ' + (data.error || 'Erreur lors de la suppression'), 'error');
        }
    } catch (error) {
        console.error('Erreur:', error);
        showNotification('Erreur lors de la suppression de l\'utilisateur', 'error');
    }
}

// Filtrer les utilisateurs
function filterUsers() {
    const searchTerm = document.getElementById('userSearch').value.toLowerCase();
    const roleFilter = document.getElementById('userRoleFilter').value;
    const statusFilter = document.getElementById('userStatusFilter').value;
    
    const filteredUsers = usersFirebase.filter(user => {
        const nom = (user.nom || '').toLowerCase();
        const email = (user.email || '').toLowerCase();
        const nomUtilisateur = (user.nomUtilisateur || '').toLowerCase();
        const matchesSearch = nom.includes(searchTerm) || email.includes(searchTerm) || nomUtilisateur.includes(searchTerm);
        const matchesRole = !roleFilter || user.role === roleFilter;
        const matchesStatus = !statusFilter || user.statut === statusFilter;
        
        return matchesSearch && matchesRole && matchesStatus;
    });
    
    const usersList = document.getElementById('usersList');
    if (!usersList) return;
    
    const isSuperAdmin = checkIfSuperAdmin();
    
    usersList.innerHTML = filteredUsers.map(user => {
        const nom = user.nom || 'Sans nom';
        const email = user.email || '';
        const telephone = user.telephone || '';
        const role = user.role || 'utilisateur';
        const statut = user.statut || 'actif';
        
        const actions = isSuperAdmin ? `
            <div style="display: flex; gap: 0.5rem; margin-top: 0.5rem;">
                <button class="btn btn-sm btn-primary" onclick="editUser('${user.userId}')">
                    <i class="fas fa-edit"></i> Modifier
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteUser('${user.userId}')">
                    <i class="fas fa-trash"></i> Supprimer
                </button>
            </div>
        ` : '';
        
        return `
        <div class="user-item">
            <div class="user-info">
                <h3>${nom}</h3>
                <p>${email}</p>
                <p>${telephone || 'Non renseigné'}</p>
                ${actions}
            </div>
            <div style="display: flex; align-items: center; gap: 1rem; flex-direction: column;">
                <span class="user-role ${role}">${getRoleText(role)}</span>
                <span class="user-status ${statut}">${statut === 'actif' ? 'Actif' : 'Inactif'}</span>
            </div>
        </div>
    `;
    }).join('');
}

// Fonctions utilitaires pour les tickets et utilisateurs
function getTicketStatusText(status) {
    const statuses = {
        open: 'Ouvert',
        in_progress: 'En cours',
        pending: 'En attente',
        resolved: 'Résolu',
        closed: 'Fermé'
    };
    return statuses[status] || status;
}

function getCategoryText(category) {
    const categories = {
        maintenance: 'Maintenance',
        repair: 'Réparation',
        inspection: 'Inspection',
        upgrade: 'Amélioration',
        other: 'Autre'
    };
    return categories[category] || category;
}

function getRoleText(role) {
    const roles = {
        admin: 'Administrateur',
        manager: 'Responsable',
        technician: 'Technicien',
        user: 'Utilisateur'
    };
    return roles[role] || role;
}

// Utilitaires
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
    
    // Réinitialiser les formulaires
    if (modalId === 'addMachineModal') {
        document.getElementById('addMachineForm').reset();
        document.getElementById('customFields').innerHTML = '';
        document.querySelector('#addMachineModal .modal-header h2').textContent = 'Ajouter une Machine';
        document.getElementById('addMachineForm').onsubmit = handleAddMachine;
    } else if (modalId === 'addCategoryModal') {
        document.getElementById('addCategoryForm').reset();
        document.querySelector('#addCategoryModal .modal-header h2').textContent = 'Ajouter une Catégorie';
        document.getElementById('addCategoryForm').onsubmit = handleAddCategory;
    } else if (modalId === 'addSubCategoryModal') {
        document.getElementById('addSubCategoryForm').reset();
        document.querySelector('#addSubCategoryModal .modal-header h2').textContent = 'Ajouter une Sous-catégorie';
        document.getElementById('addSubCategoryForm').onsubmit = handleAddSubCategory;
    } else if (modalId === 'addRepairModal') {
        document.getElementById('addRepairForm').reset();
        document.querySelector('#addRepairModal .modal-header h2').textContent = 'Nouvelle Réparation';
        document.getElementById('addRepairForm').onsubmit = handleAddRepair;
    } else if (modalId === 'addTicketModal') {
        document.getElementById('addTicketForm').reset();
        document.querySelector('#addTicketModal .modal-header h2').textContent = 'Nouveau Ticket';
        document.getElementById('addTicketForm').onsubmit = handleAddTicket;
    } else if (modalId === 'addUserModal') {
        document.getElementById('addUserForm').reset();
        document.querySelector('#addUserModal .modal-header h2').textContent = 'Ajouter un Utilisateur';
        document.getElementById('addUserForm').onsubmit = handleAddUser;
    } else if (modalId === 'addTechnicianModal') {
        document.getElementById('addTechnicianForm').reset();
        document.querySelector('#addTechnicianModal .modal-header h2').textContent = 'Ajouter un Technicien';
        document.getElementById('addTechnicianForm').onsubmit = handleAddTechnician;
    } else if (modalId === 'addInventoryModal') {
        document.getElementById('addInventoryForm').reset();
        document.querySelector('#addInventoryModal .modal-header h2').textContent = 'Ajouter une Pièce de Stock';
        document.getElementById('addInventoryForm').onsubmit = handleAddInventory;
    } else if (modalId === 'addMaintenanceModal') {
        document.getElementById('addMaintenanceForm').reset();
        document.querySelector('#addMaintenanceModal .modal-header h2').textContent = 'Programmer une Maintenance';
        document.getElementById('addMaintenanceForm').onsubmit = handleAddMaintenance;
    }
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showNotification(message, type = 'info') {
    // Créer une notification temporaire
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? 'var(--success-color)' : type === 'error' ? 'var(--danger-color)' : 'var(--primary-color)'};
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        box-shadow: var(--shadow-lg);
        z-index: 10000;
        animation: slideIn 0.3s ease;
    `;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// Ajouter les animations CSS pour les notifications
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// ===== FONCTIONNALITÉS RESPONSIVE =====

// Variables pour le responsive
let isMobileMenuOpen = false;

// Initialisation des fonctionnalités responsive
function initializeResponsive() {
    setupMobileMenu();
    setupResponsiveEvents();
    updateNotificationBadge();
}

// Configuration du menu mobile
function setupMobileMenu() {
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    if (menuToggle && sidebar && overlay) {
        menuToggle.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            toggleMobileMenu();
        });
        
        overlay.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeMobileMenu();
        });
        
        // Empêcher la fermeture en cliquant dans la sidebar
        sidebar.addEventListener('click', (e) => {
            e.stopPropagation();
        });
        
        // Fermer avec Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && isMobileMenuOpen) {
                closeMobileMenu();
            }
        });
    }
}

// Basculer le menu mobile
function toggleMobileMenu() {
    if (isMobileMenuOpen) {
        closeMobileMenu();
    } else {
        openMobileMenu();
    }
}

// Ouvrir le menu mobile
function openMobileMenu() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    if (sidebar && overlay) {
        sidebar.classList.add('open');
        overlay.classList.add('show');
        document.body.style.overflow = 'hidden';
        isMobileMenuOpen = true;
    }
}

// Fermer le menu mobile
function closeMobileMenu() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    if (sidebar && overlay) {
        sidebar.classList.remove('open');
        overlay.classList.remove('show');
        document.body.style.overflow = '';
        isMobileMenuOpen = false;
    }
}

// Configuration des événements responsive
function setupResponsiveEvents() {
    // Fermer le menu mobile lors de la navigation
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        item.addEventListener('click', () => {
            if (isMobileMenuOpen) {
                closeMobileMenu();
            }
        });
    });
    
    // Gestion du redimensionnement de la fenêtre
    window.addEventListener('resize', handleResize);
    
    // Gestion de l'orientation
    window.addEventListener('orientationchange', () => {
        setTimeout(handleResize, 100);
    });
}

// Gérer le redimensionnement
function handleResize() {
    const width = window.innerWidth;
    
    // Si on passe en mode desktop, fermer le menu mobile
    if (width > 768 && isMobileMenuOpen) {
        closeMobileMenu();
    }
    
    // Ajuster les graphiques si nécessaire
    if (typeof updateCharts === 'function') {
        setTimeout(updateCharts, 100);
    }
}

// Mettre à jour le badge de notification
function updateNotificationBadge() {
    const badge = document.getElementById('notificationBadge');
    if (badge) {
        const urgentTickets = tickets.filter(t => t.priority === 'urgent' && t.status !== 'closed').length;
        const pendingRepairs = repairs.filter(r => r.status === 'pending').length;
        const totalNotifications = urgentTickets + pendingRepairs;
        
        badge.textContent = totalNotifications;
        badge.style.display = totalNotifications > 0 ? 'flex' : 'none';
    }
}

// Fonction pour basculer le menu (accessible globalement)
function toggleMenu() {
    toggleMobileMenu();
}

// Améliorer l'accessibilité tactile
function enhanceTouchAccessibility() {
    // Augmenter la taille des éléments tactiles sur mobile
    if ('ontouchstart' in window) {
        const style = document.createElement('style');
        style.textContent = `
            @media (hover: none) and (pointer: coarse) {
                .btn {
                    min-height: 44px;
                    min-width: 44px;
                }
                
                .menu-item {
                    min-height: 44px;
                }
                
                .close {
                    min-height: 44px;
                    min-width: 44px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                
                .notification-close {
                    min-height: 44px;
                    min-width: 44px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
            }
        `;
        document.head.appendChild(style);
    }
}

// Détecter le type d'appareil
function detectDevice() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isMobile = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(userAgent);
    const isTablet = /ipad|android(?!.*mobile)/i.test(userAgent);
    
    return {
        isMobile,
        isTablet,
        isDesktop: !isMobile && !isTablet
    };
}

// Optimiser les performances sur mobile
function optimizeForMobile() {
    const device = detectDevice();
    
    if (device.isMobile) {
        // Réduire les animations sur mobile
        const style = document.createElement('style');
        style.textContent = `
            @media (max-width: 768px) {
                * {
                    animation-duration: 0.2s !important;
                    transition-duration: 0.2s !important;
                }
            }
        `;
        document.head.appendChild(style);
        
        // Optimiser les images si elles existent
        const images = document.querySelectorAll('img');
        images.forEach(img => {
            img.loading = 'lazy';
        });
    }
}

// Initialiser toutes les fonctionnalités responsive
document.addEventListener('DOMContentLoaded', function() {
    // Attendre que l'application principale soit initialisée
    setTimeout(() => {
        initializeResponsive();
        enhanceTouchAccessibility();
        optimizeForMobile();
        integrateResponsiveFeatures();
    }, 100);
});

// Fonction pour basculer entre vue tableau et cartes sur mobile
function toggleTableView() {
    const tables = document.querySelectorAll('.data-table');
    const containers = document.querySelectorAll('.table-container');
    
    tables.forEach((table, index) => {
        const container = containers[index];
        if (!container) return;
        
        // Vérifier si c'est le tableau des machines
        const isMachinesTable = table.id === 'machinesTable' || 
                               container.querySelector('#machinesTable') ||
                               table.querySelector('th')?.textContent.includes('Machine');
        
        const isMobile = window.innerWidth <= 480;
        
        if (isMobile && !isMachinesTable) {
            // Convertir en cartes sur mobile (sauf pour les machines)
            convertTableToCards(table, container);
        } else {
            // Garder le tableau sur desktop et toujours pour les machines
            convertCardsToTable(table, container);
        }
    });
}

// Convertir un tableau en cartes pour mobile
function convertTableToCards(table, container) {
    if (container.classList.contains('table-mobile-cards')) return;
    
    const tbody = table.querySelector('tbody');
    if (!tbody) return;
    
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const headers = Array.from(table.querySelectorAll('th')).map(th => th.textContent.trim());
    
    let cardsHTML = '<div class="table-mobile-cards">';
    
    rows.forEach(row => {
        const cells = Array.from(row.querySelectorAll('td'));
        if (cells.length === 0) return;
        
        cardsHTML += '<div class="mobile-card">';
        cardsHTML += '<div class="mobile-card-header">';
        cardsHTML += `<div class="mobile-card-title">${cells[0].textContent}</div>`;
        
        // Ajouter les badges/statuts si présents
        const badges = row.querySelectorAll('.priority-badge, .status-badge');
        badges.forEach(badge => {
            cardsHTML += `<span class="${badge.className}">${badge.textContent}</span>`;
        });
        
        cardsHTML += '</div>';
        cardsHTML += '<div class="mobile-card-meta">';
        
        // Afficher les informations importantes
        for (let i = 1; i < Math.min(cells.length, 4); i++) {
            if (cells[i].textContent.trim()) {
                cardsHTML += `<span><strong>${headers[i]}:</strong> ${cells[i].textContent}</span>`;
            }
        }
        
        cardsHTML += '</div>';
        
        // Ajouter les actions
        const actions = row.querySelectorAll('.btn');
        if (actions.length > 0) {
            cardsHTML += '<div class="mobile-card-actions">';
            actions.forEach(action => {
                cardsHTML += `<button class="btn ${action.className}" onclick="${action.getAttribute('onclick') || ''}">${action.innerHTML}</button>`;
            });
            cardsHTML += '</div>';
        }
        
        cardsHTML += '</div>';
    });
    
    cardsHTML += '</div>';
    
    // Remplacer le contenu
    container.innerHTML = cardsHTML;
    container.classList.add('table-mobile-cards');
}

// Convertir les cartes en tableau pour desktop
function convertCardsToTable(cardsContainer, container) {
    if (!container.classList.contains('table-mobile-cards')) return;
    
    // Restaurer le tableau original
    container.classList.remove('table-mobile-cards');
    // Le tableau sera restauré par le rechargement de la page ou par une fonction de restauration
}

// Améliorer l'affichage des filtres sur mobile
function enhanceMobileFilters() {
    const filters = document.querySelectorAll('.filters, .machine-filters, .tickets-filters, .users-filters, .inventory-filters, .repair-filters');
    
    filters.forEach(filter => {
        // Ajouter des labels visuels pour mobile
        const inputs = filter.querySelectorAll('input, select');
        inputs.forEach(input => {
            if (!input.previousElementSibling || !input.previousElementSibling.classList.contains('filter-label')) {
                const label = document.createElement('label');
                label.className = 'filter-label';
                label.textContent = input.placeholder || input.name || 'Filtre';
                label.style.display = 'none';
                input.parentNode.insertBefore(label, input);
            }
        });
    });
}

// Afficher les labels des filtres sur mobile
function showFilterLabels() {
    const isMobile = window.innerWidth <= 768;
    const labels = document.querySelectorAll('.filter-label');
    
    labels.forEach(label => {
        label.style.display = isMobile ? 'block' : 'none';
        if (isMobile) {
            label.style.fontSize = '0.8rem';
            label.style.fontWeight = '500';
            label.style.color = 'var(--text-secondary)';
            label.style.marginBottom = '0.25rem';
        }
    });
}

// Mettre à jour l'affichage responsive
function updateResponsiveDisplay() {
    toggleTableView();
    showFilterLabels();
    enhanceMobileFilters();
}

// Améliorer l'affichage du tableau des machines sur mobile
function enhanceMachinesTable() {
    const machinesTable = document.getElementById('machinesTable');
    if (!machinesTable) return;
    
    const isMobile = window.innerWidth <= 768;
    
    if (isMobile) {
        // Ajouter des tooltips pour les cellules tronquées
        const cells = machinesTable.querySelectorAll('td');
        cells.forEach(cell => {
            const text = cell.textContent.trim();
            if (text.length > 15) {
                cell.setAttribute('title', text);
                cell.style.cursor = 'help';
            }
        });
        
        // Optimiser les boutons d'action
        const actionButtons = machinesTable.querySelectorAll('.btn');
        actionButtons.forEach(btn => {
            btn.style.minWidth = 'auto';
            btn.style.padding = '0.25rem 0.5rem';
            btn.style.fontSize = '0.7rem';
        });
        
        // Ajouter un indicateur de scroll horizontal
        const container = machinesTable.closest('.table-container');
        if (container && !container.querySelector('.scroll-indicator')) {
            const indicator = document.createElement('div');
            indicator.className = 'scroll-indicator';
            indicator.innerHTML = '<i class="fas fa-arrows-alt-h"></i> Faites défiler horizontalement';
            indicator.style.cssText = `
                text-align: center;
                padding: 0.5rem;
                background: var(--background-color);
                color: var(--text-secondary);
                font-size: 0.8rem;
                border-top: 1px solid var(--border-color);
                display: block;
            `;
            container.appendChild(indicator);
        }
    } else {
        // Supprimer les indicateurs sur desktop
        const indicators = document.querySelectorAll('.scroll-indicator');
        indicators.forEach(indicator => indicator.remove());
    }
}

// Intégrer les fonctions responsive dans le système existant
function integrateResponsiveFeatures() {
    // Mettre à jour l'affichage au chargement
    updateResponsiveDisplay();
    enhanceMachinesTable();
    
    // Mettre à jour lors du redimensionnement
    window.addEventListener('resize', () => {
        setTimeout(() => {
            updateResponsiveDisplay();
            enhanceMachinesTable();
        }, 100);
    });
    
    // Mettre à jour après les actions qui modifient les tableaux
    const originalUpdateDashboard = updateDashboard;
    updateDashboard = function() {
        originalUpdateDashboard();
        setTimeout(() => {
            updateResponsiveDisplay();
            enhanceMachinesTable();
        }, 100);
    };
    
    // Améliorer le tableau des machines après le rendu
    const originalRenderMachinesTable = renderMachinesTable;
    renderMachinesTable = function() {
        originalRenderMachinesTable();
        setTimeout(enhanceMachinesTable, 100);
    };
}

// Fonction pour gérer la déconnexion
function handleLogout() {
    // Supprimer les données de session locale
    localStorage.removeItem('currentUser');
    localStorage.removeItem('currentEnterprise');
    
    // Appeler l'endpoint de déconnexion Spring Boot (redirige vers /login)
    window.location.href = '/logout';
}

// Fonction pour afficher les entreprises dans la page de gestion
function renderEnterprises() {
    const enterprisesList = document.getElementById('enterprisesList');
    if (!enterprisesList) return;
    
    enterprisesList.innerHTML = '';
    
    enterprises.forEach(enterprise => {
        const isCurrent = currentEnterprise && enterprise.id === currentEnterprise.id;
        const item = document.createElement('div');
        item.className = 'enterprise-item';
        if (isCurrent) {
            item.style.border = '3px solid var(--primary-color)';
            item.style.boxShadow = '0 4px 12px rgba(59, 130, 246, 0.3)';
        }
        
        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 1rem; flex: 1;">
                <i class="fas fa-building" style="font-size: 2rem; color: var(--primary-color);"></i>
                <div class="enterprise-info" style="flex: 1;">
                    <h3 style="margin: 0 0 0.5rem 0; display: flex; align-items: center; gap: 0.5rem;">
                        ${enterprise.name}
                        ${isCurrent ? '<span class="badge-success" style="margin-left: 0.5rem;"><i class="fas fa-check-circle"></i> Actuelle</span>' : ''}
                    </h3>
                    <p style="margin: 0.25rem 0; color: var(--text-secondary);">
                        <i class="fas fa-map-marker-alt"></i> ${enterprise.address}, ${enterprise.postalCode} ${enterprise.city}
                    </p>
                    <p style="margin: 0.25rem 0; color: var(--text-secondary); font-size: 0.85rem;">
                        <i class="fas fa-phone"></i> ${enterprise.phone} | 
                        <i class="fas fa-envelope"></i> ${enterprise.email}
                    </p>
                </div>
            </div>
            <div style="display: flex; gap: 0.5rem; align-items: center;">
                ${!isCurrent ? 
                    `<button class="btn btn-primary" onclick="switchEnterprise(${enterprise.id})">
                        <i class="fas fa-exchange-alt"></i> Sélectionner
                    </button>` : 
                    '<span style="padding: 0.5rem 1rem; background: var(--success-color); color: white; border-radius: 4px; font-size: 0.85rem;">Sélectionnée</span>'
                }
            </div>
        `;
        enterprisesList.appendChild(item);
    });
}

// Filtrer les entreprises
function filterEnterprises() {
    const searchTerm = document.getElementById('enterpriseSearch').value.toLowerCase();
    const enterpriseItems = document.querySelectorAll('.enterprise-item');
    
    enterpriseItems.forEach(item => {
        const text = item.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
}

// Appeler renderEnterprises() quand on affiche la page entreprises
document.addEventListener('DOMContentLoaded', function() {
    const menuItems = document.querySelectorAll('.menu-item');
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            const page = this.dataset.page;
            if (page === 'enterprises') {
                renderEnterprises();
            }
        });
    });
});

// Exposer les fonctions globalement pour l'accessibilité
window.toggleMenu = toggleMenu;
window.closeMobileMenu = closeMobileMenu;
window.toggleTableView = toggleTableView;
window.handleLogout = handleLogout;
window.showAddEnterpriseModal = showAddEnterpriseModal;
window.changeEnterprise = changeEnterprise;
window.switchEnterprise = switchEnterprise;
window.filterEnterprises = filterEnterprises;