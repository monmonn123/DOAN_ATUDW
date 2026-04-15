package com.edumoet.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.edumoet.service.common.ActivityLogService;
import com.edumoet.service.common.ImageService;

@Controller("managerImageController")
@RequestMapping("/manager/images")
public class ImageController {

    private final ImageService imageService;
    private final ActivityLogService logService;

    public ImageController(ImageService imageService, ActivityLogService logService) {
        this.imageService = imageService;
        this.logService = logService;
    }

    @PostMapping("/upload/question/{questionId}")
    public String uploadQuestionImage(@PathVariable Long questionId,
                                      @RequestParam MultipartFile file,
                                      @RequestParam Long uploaderId) {
        imageService.uploadForQuestion(file, questionId, uploaderId);
        logService.logAction(uploaderId, "UPLOAD_QUESTION_IMAGE", "IMAGE", questionId, file.getOriginalFilename());
        return "redirect:/manager/questions/" + questionId;
    }

    @PostMapping("/upload/answer/{answerId}")
    public String uploadAnswerImage(@PathVariable Long answerId,
                                    @RequestParam MultipartFile file,
                                    @RequestParam Long uploaderId) {
        imageService.uploadForAnswer(file, answerId, uploaderId);
        logService.logAction(uploaderId, "UPLOAD_ANSWER_IMAGE", "IMAGE", answerId, file.getOriginalFilename());
        return "redirect:/answers/" + answerId;
    }
}