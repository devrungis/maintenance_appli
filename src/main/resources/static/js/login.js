// Gérer la connexion simple sans sélection d'entreprise

// Afficher une erreur
function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    const errorText = document.getElementById('errorText');
    errorText.textContent = message;
    errorDiv.classList.add('show');
    
    setTimeout(() => {
        errorDiv.classList.remove('show');
    }, 5000);
}

// Gérer la connexion
document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    // Vérifier les identifiants (pour l'instant, accepte tout)
    if (!username || !password) {
        showError('Veuillez remplir tous les champs');
        return;
    }
    
    // Sauvegarder l'utilisateur
    localStorage.setItem('currentUser', JSON.stringify({
        username: username,
        loginDate: new Date().toISOString()
    }));
    
    // Rediriger vers l'application principale
    window.location.href = 'index.html';
});
