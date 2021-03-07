package net.iceyleagons.resourcepackhost;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import net.iceyleagons.resourcepackhost.service.HostService;
import net.iceyleagons.resourcepackhost.service.RateLimitService;
import net.iceyleagons.resourcepackhost.service.ReCaptchaValidationService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Controller
@PropertySource("classpath:local.properties")
public class HtmlController {

    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private ReCaptchaValidationService validationService;

    @Autowired
    private HostService hostService;

    @Autowired
    private Environment environment;

    @RequestMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("siteKey", environment.getProperty("recaptcha.sitekey"));
        return "index";
    }

    @PostMapping(value = "/userupload")
    public String upload(@RequestParam(name="g-recaptcha-response") String captcha, @RequestParam("file") MultipartFile file, HttpServletRequest request,
                         Model model) {
        model.addAttribute("siteKey", environment.getProperty("recaptcha.sitekey"));
        if (validationService.validateCaptcha(captcha)) {
            try {
                if (file.isEmpty()) {
                    model.addAttribute("error", "Empty file!");
                    return "index";
                }
                String extension = FilenameUtils.getExtension(file.getOriginalFilename());
                if (extension == null) throw new IllegalStateException();

                if (!extension.equalsIgnoreCase("zip")) {
                    model.addAttribute("error", "Unsupported media type!");
                    return "index";
                }

                if (file.getSize() >= APIController.MB_LIMIT) {
                    model.addAttribute("error", "Upload limit exceeded!");
                    return "index";
                }

                Bucket bucket = rateLimitService.get(request.getRemoteAddr());
                ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getUploadCost());

                if (consumptionProbe.isConsumed()) {

                    model.addAttribute("success", "Upload success! Your file is accessible via: " + request.getRequestURL().toString().replaceAll("/userupload", "/get/"+hostService.upload(file)));
                    return "index";
                }
                model.addAttribute("error", "Too many requests!");
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error", "Internal error!");
            }
        } else {
            model.addAttribute("error", "Invalid captcha!");
        }

        return "index";
    }

}
