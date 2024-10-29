package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.xml.ForecastDto;
import softuni.exam.models.dto.xml.ForecastRootDto;
import softuni.exam.models.entity.City;
import softuni.exam.models.entity.Forecast;
import softuni.exam.models.entity.some.DayOfWeek;
import softuni.exam.repository.CityRepository;
import softuni.exam.repository.ForecastRepository;
import softuni.exam.service.ForecastService;
import softuni.exam.util.ValidationUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;


@Service
public class ForecastServiceImpl implements ForecastService {
    private static final String FILE_PATH = "C:\\Users\\ilievpet\\Desktop\\SoftWeather Forecast_Skeleton\\skeleton\\src\\main\\resources\\files\\xml\\forecasts.xml";
    private final ForecastRepository forecastRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final CityRepository cityRepository;

    public ForecastServiceImpl(ForecastRepository forecastRepository, ModelMapper modelMapper, ValidationUtil validationUtil, CityRepository cityRepository) {
        this.forecastRepository = forecastRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.cityRepository = cityRepository;
    }

    @Override
    public boolean areImported() {
        return this.forecastRepository.count() > 0;
    }

    @Override
    public String readForecastsFromFile() throws IOException {
        return new String(Files.readAllBytes(Path.of(FILE_PATH)));
    }

    @Override
    public String importForecasts() throws IOException, JAXBException {
        StringBuilder sb = new StringBuilder();

        JAXBContext context = JAXBContext.newInstance(ForecastRootDto.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ForecastRootDto forecastRootDto = (ForecastRootDto) unmarshaller.unmarshal(new File(FILE_PATH));

        for (ForecastDto forecastDto : forecastRootDto.getForecastRootDtoList()) {
            long cityId = forecastDto.getCity();

            DayOfWeek dayOfWeek = forecastDto.getDayOfWeek();
            Optional<Forecast> optionalForecast = forecastRepository.findByCityIdAndDayOfWeek(cityId, dayOfWeek);

            if (!this.validationUtil.isValid(forecastDto) || optionalForecast.isPresent()) {
                sb.append(String.format("Invalid forecast\n"));
                continue;
            }

            Forecast forecast = this.modelMapper.map(forecastDto, Forecast.class);
            City city = cityRepository.findById(cityId).get();
            forecast.setCity(city);
            this.forecastRepository.saveAndFlush(forecast);

            sb.append(String.format("Successfully imported forecast %s - %.2f\n",
                    forecast.getDayOfWeek(), forecast.getMaxTemperature()));
        }
        return sb.toString().trim();
    }

    @Override
    public String exportForecasts() {
        StringBuilder sb = new StringBuilder();
        forecastRepository.findAllByDayOfWeekAndCityPopulationLessThanOrderByMaxTemperatureDescIdAsc(DayOfWeek.SUNDAY, 150000)
                .stream()
                .forEach(forecast -> sb.append(String.format("" +
                        "City: %s:\n" +
                        "-min temperature: %.2f\n" +
                        "--max temperature: %.2f\n" +
                        "---sunrise: %s\n" +
                        "----sunset: %s\n",
                        forecast.getCity().getCityName(),
                        forecast.getMinTemperature(),
                        forecast.getMaxTemperature(),
                        forecast.getSunrise(),
                        forecast.getSunset())));
        return sb.toString().trim();
    }
}
