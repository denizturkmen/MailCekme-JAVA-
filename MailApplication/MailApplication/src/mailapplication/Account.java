/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailapplication;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Persistence;

/**
 *
 * @author 
 */
@Entity
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountName;
    private String email;
    private String password;
    private String displayName;
    private String accountType;
    private String incomingServer;
    private int incomingPort;
    private boolean incomingSSL;
    private String outgoingServer;
    private int outgoingPort;
    private boolean outgoingSSL;
    
    @OneToMany(mappedBy="relatedAccount")
    private List<Email> emails;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        String oldAccountName = this.accountName;
        this.accountName = accountName;
        propertyChangeSupport.firePropertyChange("accountName", oldAccountName, accountName);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getIncomingServer() {
        return incomingServer;
    }

    public void setIncomingServer(String incomingServer) {
        this.incomingServer = incomingServer;
    }

    public int getIncomingPort() {
        return incomingPort;
    }

    public void setIncomingPort(int incomingPort) {
        this.incomingPort = incomingPort;
    }

    public boolean isIncomingSSL() {
        return incomingSSL;
    }

    public void setIncomingSSL(boolean incomingSSL) {
        this.incomingSSL = incomingSSL;
    }

    public String getOutgoingServer() {
        return outgoingServer;
    }

    public void setOutgoingServer(String outgoingServer) {
        this.outgoingServer = outgoingServer;
    }

    public int getOutgoingPort() {
        return outgoingPort;
    }

    public void setOutgoingPort(int outgoingPort) {
        this.outgoingPort = outgoingPort;
    }

    public boolean isOutgoingSSL() {
        return outgoingSSL;
    }

    public void setOutgoingSSL(boolean outgoingSSL) {
        this.outgoingSSL = outgoingSSL;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Account)) {
            return false;
        }
        Account other = (Account) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return accountName;
    }

    private transient final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Add PropertyChangeListener.
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove PropertyChangeListener.
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    
}
