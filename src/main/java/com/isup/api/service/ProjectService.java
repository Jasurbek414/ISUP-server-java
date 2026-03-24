package com.isup.api.service;

import com.isup.entity.Project;
import com.isup.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    public List<Project> findAll() {
        return repo.findAll();
    }

    public Project findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new com.isup.isapi.IsapiException("Project not found: " + id, 404));
    }

    @Transactional
    public Project create(String name, String webhookUrl, Integer retryCount) {
        Project project = Project.builder()
                .name(name)
                .webhookUrl(webhookUrl)
                .secretKey(UUID.randomUUID().toString().replace("-", ""))
                .retryCount(retryCount != null ? retryCount : 3)
                .isActive(true)
                .build();
        return repo.save(project);
    }

    @Transactional
    public Project update(Long id, String name, String webhookUrl, Integer retryCount, Boolean active) {
        Project p = findById(id);
        if (name       != null) p.setName(name);
        if (webhookUrl != null) p.setWebhookUrl(webhookUrl);
        if (retryCount != null) p.setRetryCount(retryCount);
        if (active     != null) p.setActive(active);
        return repo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public String regenerateSecret(Long id) {
        Project p = findById(id);
        p.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
        repo.save(p);
        return p.getSecretKey();
    }
}
