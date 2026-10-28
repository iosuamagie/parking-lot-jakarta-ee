package org.example.parkinglot.ejb;

import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.parkinglot.common.CarDto;
import org.example.parkinglot.common.CarPhotoDto;
import org.example.parkinglot.entities.Car;
import org.example.parkinglot.entities.CarPhoto;
import org.example.parkinglot.entities.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class CarsBean {
    private static final Logger LOG = Logger.getLogger(CarsBean.class.getName());

    public static final int TOTAL_PARKING_SPOTS = 10;

    @PersistenceContext
    EntityManager entityManager;

    public List<CarDto> findAllCars() {
        LOG.info("findAllCars");
        try {
            TypedQuery<Car> typedQuery = entityManager.createQuery("SELECT c FROM Car c", Car.class);
            List<Car> cars = typedQuery.getResultList();
            return copyCarsToDto(cars);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    /**
     * @return {@code false} if there is no user with this id
     */
    public boolean createCar(String licensePlate, String parkingSpot, Long userId) {
        LOG.info("createCar");

        User user = entityManager.find(User.class, userId);
        if (user == null) {
            return false;
        }

        Car car = new Car();
        car.setLicensePlate(licensePlate);
        car.setParkingSpot(parkingSpot);
        user.getCars().add(car);
        car.setOwner(user);

        entityManager.persist(car);
        return true;
    }

    /**
     * @param exceptCarId the car being edited, or {@code null} for a new car
     */
    public boolean isLicensePlateTaken(String licensePlate, Long exceptCarId) {
        return countOthersWith("licensePlate", licensePlate, exceptCarId) > 0;
    }

    public boolean isParkingSpotTaken(String parkingSpot, Long exceptCarId) {
        return countOthersWith("parkingSpot", parkingSpot, exceptCarId) > 0;
    }

    private long countOthersWith(String field, String value, Long exceptCarId) {
        // field is one of our own constants, never user input
        return entityManager
                .createQuery("SELECT COUNT(c) FROM Car c WHERE c." + field + " = :value AND c.id <> :exceptId", Long.class)
                .setParameter("value", value)
                .setParameter("exceptId", exceptCarId == null ? 0L : exceptCarId)
                .getSingleResult();
    }

    private List<CarDto> copyCarsToDto(List<Car> cars) {
        List<CarDto> dtoList = new ArrayList<>();

        for (Car car : cars) {
            // A car may have no owner
            String ownerName = (car.getOwner() != null) ? car.getOwner().getUsername() : "No Owner";

            CarDto dto = new CarDto(
                    car.getId(),
                    car.getLicensePlate(),
                    car.getParkingSpot(),
                    ownerName
            );
            dtoList.add(dto);
        }
        return dtoList;
    }

    /** The cars that have a photo, so the list does not ask for photos that do not exist. */
    public Set<Long> findCarIdsWithPhoto() {
        return new HashSet<>(entityManager
                .createQuery("SELECT p.car.id FROM CarPhoto p", Long.class)
                .getResultList());
    }

    public CarDto findById(Long id) {
        // Find the entity in the database
        Car car = entityManager.find(Car.class, id);

        if (car == null) {
            return null;
        }

        // Turn the entity into a DTO for the JSP
        String ownerName = (car.getOwner() != null) ? car.getOwner().getUsername() : "";
        return new CarDto(car.getId(), car.getLicensePlate(), car.getParkingSpot(), ownerName);
    }

    /**
     * @return {@code false} if there is no car or no user with these ids
     */
    public boolean updateCar(Long carId, String licensePlate, String parkingSpot, Long userId) {
        LOG.info("updateCar");

        Car car = entityManager.find(Car.class, carId);
        User newOwner = entityManager.find(User.class, userId);
        if (car == null || newOwner == null) {
            return false;
        }
        car.setLicensePlate(licensePlate);
        car.setParkingSpot(parkingSpot);

        User oldOwner = car.getOwner();
        if (oldOwner != null) {
            oldOwner.getCars().remove(car);
        }
        newOwner.getCars().add(car);
        car.setOwner(newOwner);
        return true;
    }

    public void deleteCarsByIds(List<Long> carIds) {
        LOG.info("deleteCarsByIds");
        for (Long carId : carIds) {
            Car car = entityManager.find(Car.class, carId);
            if (car != null) {
                entityManager.remove(car);
            }
        }
    }

    /**
     * @return {@code false} if there is no car with this id
     */
    public boolean addPhotoToCar(Long carId, String filename, String fileType, byte[] fileContent) {
        LOG.info("addPhotoToCar");
        Car car = entityManager.find(Car.class, carId);
        if (car == null) {
            return false;
        }
        CarPhoto photo = new CarPhoto();
        photo.setFilename(filename);
        photo.setFileType(fileType);
        photo.setFileContent(fileContent);
        if (car.getPhoto() != null) {
            entityManager.remove(car.getPhoto());
        }
        car.setPhoto(photo);
        photo.setCar(car);
        entityManager.persist(photo);
        return true;
    }
    public CarPhotoDto findPhotoByCarId(Long carId) {
        List<CarPhoto> photos = entityManager
                .createQuery("SELECT p FROM CarPhoto p where p.car.id = :id", CarPhoto.class)
                .setParameter("id", carId)
                .getResultList();
        if (photos.isEmpty()) {
            return null;
        }
        CarPhoto photo = photos.get(0); // the first element
        return new CarPhotoDto(photo.getId(), photo.getFilename(), photo.getFileType(),
                photo.getFileContent());
    }
    public long countCars() {
        return entityManager.createQuery("SELECT COUNT(c) FROM Car c", Long.class).getSingleResult();
    }

}