package net.iceyleagons.resourcepackhost;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import net.iceyleagons.resourcepackhost.service.RateLimitService;
import net.iceyleagons.resourcepackhost.service.ReCaptchaValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Controller
public class HtmlController {

    @Autowired
    private RateLimitService rateLimitService;
    @Autowired
    private ReCaptchaValidationService validationService;

    @RequestMapping(value = "/index")
    public String index() {
        return "index";
    }

    @PostMapping(value = "/userupload")
    public String upload(@RequestParam(name="g-recaptcha-response") String captcha, @RequestParam("file") MultipartFile file, HttpServletRequest request,
                         Model model) {
        if (validationService.validateCaptcha(captcha)) {
            try {
                Bucket bucket = rateLimitService.get(request.getRemoteAddr());
                ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getUploadCost());

                if (consumptionProbe.isConsumed()) {
                    System.out.println("Success!");
                    model.addAttribute("success", "Upload success! Your file is accessible via: " + request.getRequestURL().toString().replaceAll("/userupload", "/get/asd"));
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
