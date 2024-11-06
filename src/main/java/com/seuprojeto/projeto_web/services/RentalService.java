package com.seuprojeto.projeto_web.services;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seuprojeto.projeto_web.entities.OptionalEntity;
import com.seuprojeto.projeto_web.entities.RentalEntity;
import com.seuprojeto.projeto_web.exceptions.EntityNotFoundException;
import com.seuprojeto.projeto_web.exceptions.TableEmptyException;
import com.seuprojeto.projeto_web.repositories.ClientRepository;
import com.seuprojeto.projeto_web.repositories.OptionalRepository;
import com.seuprojeto.projeto_web.repositories.RentalRepository;
import com.seuprojeto.projeto_web.repositories.VehicleRepository;
import com.seuprojeto.projeto_web.requests.RentalRequest;


@Service
public class RentalService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private OptionalRepository optionalRepository;
    @Autowired
    private RentalRepository rentalRepository;
    ModelMapper modelMapper = new ModelMapper();

    public List<RentalRequest> findAllRentals() {
        List<RentalEntity> rentalEntities = rentalRepository.findAll();
    
        if (rentalEntities.isEmpty()) {
            throw new TableEmptyException("No data registered");
        }
        
        // Converte cada RentalEntity para RentalRequest e trata o campo 'optionals'
        List<RentalRequest> rentalRequests = rentalEntities.stream()
                .map(entity -> {
                    RentalRequest rentalRequest = modelMapper.map(entity, RentalRequest.class);
                    // Converte o campo 'optionals' de JSON para lista
                    rentalRequest.setOptionals(convertJsonToOptionals(entity.getOptionals()));
                    return rentalRequest;
                })
                .collect(Collectors.toList());
    
        return rentalRequests;
    }

    public RentalRequest findRentalById(Long id) {
        Optional<RentalEntity> rentalEntityOptional = rentalRepository.findById(id);
        
        if (rentalEntityOptional.isEmpty()) {
            throw new EntityNotFoundException("Rental with ID " + id + " not found");
        }
        
        RentalEntity rentalEntity = rentalEntityOptional.get();

        RentalRequest rentalRequest = modelMapper.map(rentalEntity, RentalRequest.class);
    
        // Deserializa o campo optionals de JSON para lista
        rentalRequest.setOptionals(convertJsonToOptionals(rentalEntity.getOptionals()));

        return rentalRequest;
    }

    public RentalRequest createRental(RentalRequest rentalRequest) {
        RentalEntity rental = new RentalEntity();

        // Verificando se o cliente existe
        rental.setClient(clientRepository.findById(rentalRequest.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado")));

        // Verificando se o veículo existe
        rental.setVehicle(vehicleRepository.findById(rentalRequest.getVehicleId())
            .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado")));

        List<RentalRequest.OptionalRequest> optionals = rentalRequest.getOptionals();
        if (optionals != null && !optionals.isEmpty()) {
            for (RentalRequest.OptionalRequest optional : optionals) {
                if (!optionalRepository.existsById(optional.getOptionalId())) {
                    throw new EntityNotFoundException("Opcional com ID " + optional.getOptionalId() + " não encontrado");
                }
            }

            // Convertendo a lista de opcionais para JSON
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String optionalsJson = objectMapper.writeValueAsString(optionals);
                rental.setOptionals(optionalsJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao converter opcionais para JSON", e);
            }
        }

        rental.setRentalDateTimeStart(rentalRequest.getRentalDateTimeStart());
        rental.setRentalDateTimeEnd(rentalRequest.getRentalDateTimeEnd());
        rental.setDailyRate(rentalRequest.getDailyRate());
        rental.setTotalDays(rentalRequest.getTotalDays());
        rental.setTotalAmount(rentalRequest.getTotalAmount());
        rental.setDepositAmount(rentalRequest.getDepositAmount());
        rental.setTotalOptionalItemsValue(rentalRequest.getTotalOptionalItemsValue());
        rental.setPlateVehicle(rentalRequest.getPlateVehicle());
        rental.setInitialMileage(rentalRequest.getInitialMileage());
        rental.setReturnMileage(rentalRequest.getReturnMileage());
        rental.setActive(rentalRequest.isActive());

        rentalRepository.save(rental);

        if (optionals != null && !optionals.isEmpty()) {
            updateOptionalQuantities(optionals);
        }
        
        return rentalRequest;
    }

    public void deleteRentalbyId(Long id) {
        RentalEntity rentalEntity = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found."));
                rentalRepository.delete(rentalEntity);
    }

    public RentalRequest updateRentalById(Long id, RentalRequest rentalRequest) {
        RentalEntity rentalEntity = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental with ID " + id + " not found"));
    
        // Atualizando informações da locação
        rentalEntity.setClient(clientRepository.findById(rentalRequest.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado")));
        rentalEntity.setVehicle(vehicleRepository.findById(rentalRequest.getVehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado")));
    
        if (rentalRequest.getOptionals() != null && !rentalRequest.getOptionals().isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String optionalsJson = objectMapper.writeValueAsString(rentalRequest.getOptionals());
                rentalEntity.setOptionals(optionalsJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao converter opcionais para JSON", e);
            }
        }
    
        rentalEntity.setRentalDateTimeStart(rentalRequest.getRentalDateTimeStart());
        rentalEntity.setRentalDateTimeEnd(rentalRequest.getRentalDateTimeEnd());
        rentalEntity.setDailyRate(rentalRequest.getDailyRate());
        rentalEntity.setTotalDays(rentalRequest.getTotalDays());
        rentalEntity.setTotalAmount(rentalRequest.getTotalAmount());
        rentalEntity.setDepositAmount(rentalRequest.getDepositAmount());
        rentalEntity.setTotalOptionalItemsValue(rentalRequest.getTotalOptionalItemsValue());
        rentalEntity.setPlateVehicle(rentalRequest.getPlateVehicle());
        rentalEntity.setInitialMileage(rentalRequest.getInitialMileage());
        rentalEntity.setReturnMileage(rentalRequest.getReturnMileage());
        rentalEntity.setActive(rentalRequest.isActive());
    
        rentalRepository.save(rentalEntity);
        return rentalRequest;
    }
    
    public RentalRequest partialUpdateRentalById(Long id, RentalRequest rentalRequest) {
        RentalEntity rentalEntity = rentalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental with ID " + id + " not found"));
    
        // Atualizando apenas campos específicos, se presentes
        if (rentalRequest.getClientId() != null) {
            rentalEntity.setClient(clientRepository.findById(rentalRequest.getClientId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado")));
        }
    
        if (rentalRequest.getVehicleId() != null) {
            rentalEntity.setVehicle(vehicleRepository.findById(rentalRequest.getVehicleId())
                    .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado")));
        }
    
        // Atualizar opcionais se a lista não estiver vazia
        if (rentalRequest.getOptionals() != null && !rentalRequest.getOptionals().isEmpty()) {
            for (RentalRequest.OptionalRequest optional : rentalRequest.getOptionals()) {
                if (!optionalRepository.existsById(optional.getOptionalId())) {
                    throw new EntityNotFoundException("Opcional com ID " + optional.getOptionalId() + " não encontrado");
                }
            }
    
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String optionalsJson = objectMapper.writeValueAsString(rentalRequest.getOptionals());
                rentalEntity.setOptionals(optionalsJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Erro ao converter opcionais para JSON", e);
            }
            
        }
    
        // Atualizando outros campos se presentes
        if (rentalRequest.getRentalDateTimeStart() != null) {
            rentalEntity.setRentalDateTimeStart(rentalRequest.getRentalDateTimeStart());
        }
        if (rentalRequest.getRentalDateTimeEnd() != null) {
            rentalEntity.setRentalDateTimeEnd(rentalRequest.getRentalDateTimeEnd());
        }
        if (rentalRequest.getDailyRate() != null) {
            rentalEntity.setDailyRate(rentalRequest.getDailyRate());
        }
        if (rentalRequest.getTotalDays() != null) {
            rentalEntity.setTotalDays(rentalRequest.getTotalDays());
        }
        if (rentalRequest.getTotalAmount() != null) {
            rentalEntity.setTotalAmount(rentalRequest.getTotalAmount());
        }
        if (rentalRequest.getDepositAmount() != null) {
            rentalEntity.setDepositAmount(rentalRequest.getDepositAmount());
        }
        if (rentalRequest.getTotalOptionalItemsValue() != null) {
            rentalEntity.setTotalOptionalItemsValue(rentalRequest.getTotalOptionalItemsValue());
        }
        if (rentalRequest.getPlateVehicle() != null) {
            rentalEntity.setPlateVehicle(rentalRequest.getPlateVehicle());
        }
        if (rentalRequest.getInitialMileage() != null) {
            rentalEntity.setInitialMileage(rentalRequest.getInitialMileage());
        }
        if (rentalRequest.getReturnMileage() != null) {
            rentalEntity.setReturnMileage(rentalRequest.getReturnMileage());
        }
        rentalEntity.setActive(rentalRequest.isActive());
    
        rentalRepository.save(rentalEntity);

        RentalRequest rental = modelMapper.map(rentalEntity, RentalRequest.class);

        // Deserializa o campo optionals de JSON para lista
        rental.setOptionals(convertJsonToOptionals(rentalEntity.getOptionals()));
    
        return rental;
    }

    private void updateOptionalQuantities(List<RentalRequest.OptionalRequest> optionals) {
        for (RentalRequest.OptionalRequest optional : optionals) {
            OptionalEntity optionalEntity = optionalRepository.findById(optional.getOptionalId())
                .orElseThrow(() -> new EntityNotFoundException("Opcional com ID " + optional.getOptionalId() + " não encontrado"));

            // Atualizando a quantidade
            int newQuantity = optionalEntity.getQtdAvailable() - optional.getQuantity();
            if (newQuantity < 0) {
                throw new RuntimeException("Quantidade do opcional " + optional.getOptionalId() + " insuficiente no estoque.");
            }
            optionalEntity.setQtdAvailable(newQuantity);
            optionalRepository.save(optionalEntity); // Salva a atualização da quantidade
        }
    }

    // Método para converter JSON em lista de OptionalRequest
    public List<RentalRequest.OptionalRequest> convertJsonToOptionals(String jsonOptionals) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonOptionals, new TypeReference<List<RentalRequest.OptionalRequest>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert JSON to Optionals", e);
        }
    }

}
