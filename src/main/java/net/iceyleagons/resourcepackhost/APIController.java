package net.iceyleagons.resourcepackhost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import net.iceyleagons.resourcepackhost.service.HostService;
import net.iceyleagons.resourcepackhost.service.RateLimitService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 */
@RestController
public class APIController {

    public static final long MB = 20;
    public static final long MB_LIMIT = MB * 1000000L;

    @Autowired
    private HostService hostService;

    @Autowired
    private RateLimitService rateLimitService;

    @PostMapping(value = "/api/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        try {
            if (file.isEmpty()) {
                objectNode.put("error", "Empty file!");
                return new ResponseEntity<>(objectNode.toString(), HttpStatus.BAD_REQUEST);
            }
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if (extension == null) throw new IllegalStateException();

            if (!extension.equalsIgnoreCase("zip")) {
                objectNode.put("error", "Unsupported media type! Supported type is: .zip");
                return new ResponseEntity<>(objectNode.toString(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            if (file.getSize() >= MB_LIMIT) {
                objectNode.put("error", "Upload limit exceeded! Please do not go over " + MB + " MBs!");
                return new ResponseEntity<>(objectNode.toString(), HttpStatus.PAYLOAD_TOO_LARGE);
            }


            Bucket bucket = rateLimitService.get(request.getRemoteAddr());

            ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getUploadCost());

            if (consumptionProbe.isConsumed()) {
                objectNode.put("id", hostService.upload(file));
                objectNode.put("remaining-tokens", consumptionProbe.getRemainingTokens());
                return new ResponseEntity<>(objectNode.toString(), HttpStatus.OK);
            }

            objectNode.put("error", "Too many requests!");
            objectNode.put("refill", TimeUnit.NANOSECONDS.toSeconds(consumptionProbe.getNanosToWaitForRefill()));
            return new ResponseEntity<>(objectNode.toString(), HttpStatus.TOO_MANY_REQUESTS);

        } catch (Exception e) {
            objectNode.put("error", "Internal server error!");
            return new ResponseEntity<>(objectNode.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/api/exists/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exists(@PathVariable String id, HttpServletRequest request) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();

            Bucket bucket = rateLimitService.getOther(request.getRemoteAddr());
            ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getDownloadRateLimit());

            if (consumptionProbe.isConsumed()) {
                byte[] data = hostService.retrieve(id);
                if (data == null) {
                    objectNode.put("exists", false);
                    objectNode.put("checksum", "");
                    return new ResponseEntity<>(objectNode.toString(), HttpStatus.NOT_FOUND);
                }

                objectNode.put("exists", true);
                objectNode.put("checksum", hostService.getChecksum(data));
                return new ResponseEntity<>(objectNode.toString(), HttpStatus.FOUND);
            }

            return new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping( value = "/get/{id:.+}", produces = MediaType.MULTIPART_MIXED_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> serveFile(@PathVariable String id, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        try {
            Bucket bucket = rateLimitService.getDownload(request.getRemoteAddr());

            ConsumptionProbe consumptionProbe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getDownloadRateLimit());

            if (consumptionProbe.isConsumed()) {
                byte[] data = hostService.retrieve(id);
                if (data == null) return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);



                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + id + ".zip\"").body(data);
            }

            return new ResponseEntity<>(null, HttpStatus.TOO_MANY_REQUESTS);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
