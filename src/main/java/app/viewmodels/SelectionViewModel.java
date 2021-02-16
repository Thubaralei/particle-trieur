/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.app.viewmodels;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import main.java.app.helpers.CSEvent;
import main.java.app.models.Supervisor;
import main.java.app.models.project.Particle;
import main.java.app.services.ImageProcessingService;
import main.java.app.models.processing.ParticleImage;

import java.util.List;

import com.google.inject.Inject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.opencv.core.Mat;

/**
 *
 * @author Ross Marchant <ross.g.marchant@gmail.com>
 */
public class SelectionViewModel {

    public CSEvent currentParticleUpdatedEvent = new CSEvent<>();
    public CSEvent<Boolean> nextImageRequested = new CSEvent<>();
    public CSEvent<Boolean> previousImageRequested = new CSEvent<>();

    public CSEvent<Integer> controlSelectIndex = new CSEvent<Integer>();
    public CSEvent<Integer> selectIndex = new CSEvent<Integer>();
    public CSEvent<int[]> selectIndicesRequested = new CSEvent<>();
    public CSEvent<int[]> shiftSelectIndices = new CSEvent<>();
    public CSEvent selectAllRequested = new CSEvent<>();

    public CSEvent increaseSizeRequested = new CSEvent<>();
    public CSEvent decreaseSizeRequested = new CSEvent<>();

    public SortedList<Particle> sortedList;
    public FilteredList<Particle> filteredList;

    //Current tab
    public int selectedTabIndex = 0;
    
    //Current particle
    private final ObjectProperty<Particle> currentParticle = new SimpleObjectProperty<>();
    public ObjectProperty<Particle> currentParticleProperty() { return currentParticle; }
    private void setCurrentParticle(Particle value) { currentParticle.set(value); }
    public Particle getCurrentParticle() { return currentParticle.get(); }
    
    //Current particles
    private final ObservableList<Particle> currentParticles = FXCollections.observableArrayList();
    public ObservableList<Particle> getCurrentParticles() { return currentParticles; }
    public void setCurrentParticles(List<Particle> values) { currentParticles.clear(); currentParticles.addAll(values); }

    //Current particle image
    private final ObjectProperty<ParticleImage> currentParticleImage = new SimpleObjectProperty<>();
    public ObjectProperty<ParticleImage> currentParticleImageProperty() { return currentParticleImage; }
    private void setCurrentParticleImage(ParticleImage value) { currentParticleImage.set(value); }
    public ParticleImage getCurrentParticleImage() { return currentParticleImage.get(); }

    // Parts
    private Supervisor supervisor;
    private ImageProcessingService imageProcessingService;
    public BooleanProperty getImageProcessingIsRunningProperty() {
        return imageProcessingService.runningProperty();
    }

    @Inject
    public SelectionViewModel(Supervisor supervisor) {
        this.supervisor = supervisor;
        filteredList = new FilteredList<>(supervisor.project.particles, p -> true);
        sortedList = new SortedList<>(filteredList);

        imageProcessingService = new ImageProcessingService(supervisor.FCNNSegmenterService);

        currentParticles.addListener((ListChangeListener<? super Particle>) listener -> {
            if (getCurrentParticles().size() > 0) {
                setCurrentParticle(getCurrentParticles().get(0));
            } else {
                setCurrentParticle(null);
            }
        });

        currentParticleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getFile().exists()) {
                Mat mat = newValue.getMat();
                if (mat != null) {
                    imageProcessingService.processAsyncAndReleaseMat(mat, supervisor.project.processingInfo, currentParticleImage);
                }
            }
            else {
                setCurrentParticleImage(null);
            }
        });
    }

    public int getParticleIndex(Particle particle) {
        return supervisor.project.getParticles().indexOf(particle);
    }

    public void refreshParticleImage() {
        if (getCurrentParticle() != null) {
            Mat mat = getCurrentParticle().getMat();
            if (mat != null) {
                imageProcessingService.processAsyncAndReleaseMat(mat, supervisor.project.processingInfo, currentParticleImage);
            }
        }
    }

    public void checkIfCurrentWasUpdated(Particle particle) {
        if (particle == getCurrentParticle()) currentParticleUpdatedEvent.broadcast();
    }

    public void checkIfCurrentWasUpdated(List<Particle> particles) {
        if (particles.contains(getCurrentParticle())) currentParticleUpdatedEvent.broadcast();
    }
}