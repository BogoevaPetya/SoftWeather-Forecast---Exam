package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.json.CityDto;
import softuni.exam.models.entity.City;
import softuni.exam.models.entity.Country;
import softuni.exam.repository.CityRepository;
import softuni.exam.repository.CountryRepository;
import softuni.exam.service.CityService;
import softuni.exam.util.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class CityServiceImpl implements CityService {
    private static final String FILE_PATH = "C:\\Users\\ilievpet\\Desktop\\SoftWeather Forecast_Skeleton\\skeleton\\src\\main\\resources\\files\\json\\cities.json";
    private final CityRepository  cityRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final CountryRepository countryRepository;

    public CityServiceImpl(CityRepository cityRepository, ModelMapper modelMapper, Gson gson, ValidationUtil validationUtil, CountryRepository countryRepository) {
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validationUtil = validationUtil;
    }

    @Override
    public boolean areImported() {
        return this.cityRepository.count() > 0;
    }

    @Override
    public String readCitiesFileContent() throws IOException {
        return new String(Files.readAllBytes(Path.of(FILE_PATH)));
    }

    @Override
    public String importCities() throws IOException {
        StringBuilder sb = new StringBuilder();
        CityDto[] cityDtos = this.gson.fromJson(readCitiesFileContent(), CityDto[].class);

        for (CityDto cityDto : cityDtos) {
            String cityName = cityDto.getCityName();
            Optional<City> optionalCity = this.cityRepository.findByCityName(cityName);

            long countryId = cityDto.getCountry();
            Optional<Country> optionalCountry = countryRepository.findById(countryId);

            if(!this.validationUtil.isValid(cityDto) || optionalCity.isPresent() || optionalCountry.isEmpty()){
                sb.append(String.format("Invalid city\n"));
                continue;
            }

            City city = this.modelMapper.map(cityDto, City.class);
            city.setCountry(optionalCountry.get());
            this.cityRepository.saveAndFlush(city);

            sb.append(String.format("Successfully imported city %s - %d\n",
                    city.getCityName(), city.getPopulation()));
        }
        return sb.toString();
    }
}
