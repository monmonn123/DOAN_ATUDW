package com.edumoet.service.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edumoet.entity.Answer;
import com.edumoet.entity.Question;
import com.edumoet.entity.User;
import com.edumoet.repository.AnswerRepository;
import com.edumoet.repository.QuestionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AnswerService {

    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private QuestionRepository questionRepository;

    public Answer createAnswer(Answer answer) {
        answer.setVotes(0);
        answer.setIsAccepted(false);
        
        Answer savedAnswer = answerRepository.save(answer);
        
        // Increment answer count
        Question question = answer.getQuestion();
        if (question != null) {
            question.setAnswerCount(question.getAnswerCount() + 1);
            questionRepository.save(question);
        }
        
        return savedAnswer;
    }

    public Optional<Answer> findById(Long id) {
        return answerRepository.findById(id);
    }

    public List<Answer> getAnswersByQuestion(Question question) {
        return answerRepository.findByQuestionOrderByVotesDescCreatedAtDesc(question);
    }

    public Page<Answer> getAnswersByAuthor(User author, Pageable pageable) {
        return answerRepository.findByAuthor(author, pageable);
    }

    public Answer updateAnswer(Answer answer) {
        return answerRepository.save(answer);
    }

    public void deleteAnswer(Long id) {
        Optional<Answer> answerOpt = answerRepository.findById(id);
        if (answerOpt.isPresent()) {
            Answer answer = answerOpt.get();
            Question question = answer.getQuestion();
            
            // Decrement answer count
            if (question != null && question.getAnswerCount() > 0) {
                question.setAnswerCount(question.getAnswerCount() - 1);
                questionRepository.save(question);
            }
            
            answerRepository.deleteById(id);
        }
    }

    public void acceptAnswer(Answer answer) {
        Question question = answer.getQuestion();
        
        // Unaccept previous answer if exists
        if (question.getAcceptedAnswer() != null) {
            Answer previousAccepted = question.getAcceptedAnswer();
            previousAccepted.setIsAccepted(false);
            answerRepository.save(previousAccepted);
        }
        
        // Accept new answer
        answer.setIsAccepted(true);
        question.setAcceptedAnswer(answer);
        answerRepository.save(answer);
    }

    /**
     * SIMPLE UPVOTE LOGIC (Toggle):
     * - Chưa vote → Click upvote → +1 vote
     * - Đã vote → Click upvote → -1 vote (undo)
     * 
     * Example: 13 votes → user click → 14 votes → user click again → 13 votes
     */
    public void upvoteAnswer(Answer answer, User user) {
        boolean hasUpvoted = user.getVotedAnswers().contains(answer);
        
        System.out.println("🔍 Upvote Answer #" + answer.getId() + " by User #" + user.getId());
        System.out.println("   Current votes: " + answer.getVotes());
        System.out.println("   Has upvoted: " + hasUpvoted);
        
        if (hasUpvoted) {
            // Already upvoted → Undo upvote (-1)
            System.out.println("   ➡️ Undo upvote: " + answer.getVotes() + " - 1 = " + (answer.getVotes() - 1));
            answer.downvote();
            user.getVotedAnswers().remove(answer);
        } else {
            // Not voted yet → Add upvote (+1)
            System.out.println("   ➡️ New upvote: " + answer.getVotes() + " + 1 = " + (answer.getVotes() + 1));
            answer.upvote();
            user.getVotedAnswers().add(answer);
        }
        
        System.out.println("   ✅ Final votes: " + answer.getVotes());
        answerRepository.save(answer);
    }

    /**
     * Downvote (Undo upvote):
     * - Chỉ hoạt động khi user ĐÃ upvote trước đó
     * - Giảm 1 vote và remove khỏi votedAnswers
     */
    public void downvoteAnswer(Answer answer, User user) {
        boolean hasUpvoted = user.getVotedAnswers().contains(answer);
        
        System.out.println("🔽 Downvote Answer #" + answer.getId() + " by User #" + user.getId());
        System.out.println("   Current votes: " + answer.getVotes());
        System.out.println("   Has upvoted: " + hasUpvoted);
        
        if (hasUpvoted) {
            // Đã upvote → Downvote = Undo (-1)
            System.out.println("   ➡️ Downvote (undo upvote): " + answer.getVotes() + " - 1 = " + (answer.getVotes() - 1));
            answer.downvote();
            user.getVotedAnswers().remove(answer);
        } else {
            // Chưa upvote → Không cho downvote
            System.out.println("   ⚠️ Cannot downvote - must upvote first");
            throw new RuntimeException("Bạn phải upvote trước khi downvote");
        }
        
        System.out.println("   ✅ Final votes: " + answer.getVotes());
        answerRepository.save(answer);
    }

    public Long countByAuthor(User author) {
        return answerRepository.countByAuthor(author);
    }

    public Long countByQuestion(Question question) {
        return answerRepository.countByQuestion(question);
    }
    
    public long countAll() {
        return answerRepository.count();
    }
    
    // ================== ADMIN FEATURES ==================
    
    public Page<Answer> getAllAnswers(Pageable pageable) {
        return answerRepository.findAll(pageable);
    }
    
    public Page<Answer> searchAnswers(String keyword, Pageable pageable) {
        return answerRepository.findByBodyContaining(keyword, pageable);
    }
    
    /**
     * Save answer (for updating existing answer)
     */
    public Answer save(Answer answer) {
        return answerRepository.save(answer);
    }
    
    /**
     * Count answers created within date range
     */
    public long countByDateRange(LocalDateTime start, LocalDateTime end) {
        return answerRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null)
                .filter(a -> !a.getCreatedAt().isBefore(start) && !a.getCreatedAt().isAfter(end))
                .count();
    }
}

