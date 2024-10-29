package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.json.CountryDto;
import softuni.exam.models.entity.Country;
import softuni.exam.repository.CountryRepository;
import softuni.exam.service.CountryService;
import softuni.exam.util.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class CountryServiceImpl implements CountryService {
    private static final String FILE_PATH = "C:\\Users\\ilievpet\\Desktop\\SoftWeather Forecast_Skeleton\\skeleton\\src\\main\\resources\\files\\json\\countries.json";
    private final CountryRepository countryRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;
    private final ValidationUtil validationUtil;

    public CountryServiceImpl(CountryRepository countryRepository, ModelMapper modelMapper, Gson gson, ValidationUtil validationUtil) {
        this.countryRepository = countryRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validationUtil = validationUtil;
    }

    @Override
    public boolean areImported() {
        return this.countryRepository.count() > 0;
    }

    @Override
    public String readCountriesFromFile() throws IOException {
        return new String(Files.readAllBytes(Path.of(FILE_PATH)));
    }

    @Override
    public String importCountries() throws IOException {
        StringBuilder sb = new StringBuilder();
        CountryDto[] countryDto = this.gson.fromJson(readCountriesFromFile(), CountryDto[].class);
        for (CountryDto dto : countryDto) {
            String countryName = dto.getCountryName();
            Optional<Country> optionalCountry = this.countryRepository.findByCountryName(countryName);

            if (!this.validationUtil.isValid(dto) || optionalCountry.isPresent()){
                sb.append(String.format("Invalid country\n"));
                continue;
            }

            Country country = modelMapper.map(dto, Country.class);
            countryRepository.saveAndFlush(country);

            sb.append(String.format("Successfully imported %s - %s\n",
                    country.getCountryName(), country.getCurrency()));
        }
        return sb.toString();
    }
}
