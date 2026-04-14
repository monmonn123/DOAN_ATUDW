package com.edumoet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false, columnDefinition = "NVARCHAR(250)")
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(unique = true, nullable = false, columnDefinition = "NVARCHAR(250)")
    private String email;

    @NotBlank
    @Size(min = 6)
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String password;

    @Column(name = "profile_image", columnDefinition = "NVARCHAR(250)")
    private String profileImage;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String about;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String bio;

    @Column(columnDefinition = "NVARCHAR(250)")
    private String location;

    @Column(columnDefinition = "NVARCHAR(250)")
    private String website;

    @Column(columnDefinition = "NVARCHAR(250)")
    private String githubUrl;

    @Column(columnDefinition = "NVARCHAR(250)")
    private String linkedinUrl;

    @Column(nullable = false, columnDefinition = "NVARCHAR(50)")
    private String role = "USER";

    @Column(nullable = false)
    private Integer reputation = 1;

    @Column(nullable = false)
    private Integer views = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    // Track UPVOTES (Toggle: user vote hoặc undo)
    @ManyToMany
    @JoinTable(
        name = "user_votes_questions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> votedQuestions = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "user_votes_answers",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "answer_id")
    )
    private Set<Answer> votedAnswers = new HashSet<>();

    @Column(nullable = false)
    private Integer points = 0;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isBanned = false;

    private LocalDateTime bannedUntil;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String banReason;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(columnDefinition = "NVARCHAR(250)")
    private String twoFactorSecret;

    @ManyToMany
    @JoinTable(
        name = "user_following",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> following = new HashSet<>();

    @ManyToMany(mappedBy = "following")
    private Set<User> followers = new HashSet<>();

    public void addPoints(Integer amount) {
        this.points += amount;
        updateLevel();
    }

    public void deductPoints(Integer amount) {
        this.points = Math.max(0, this.points - amount);
        updateLevel();
    }

    private void updateLevel() {
        // Simple level calculation: level = points / 100 + 1
        this.level = (this.points / 100) + 1;
    }

    public boolean isBanned() {
        if (!isBanned) return false;
        if (bannedUntil != null && LocalDateTime.now().isAfter(bannedUntil)) {
            isBanned = false;
            bannedUntil = null;
            banReason = null;
            return false;
        }
        return true;
    }

    public void follow(User user) {
        following.add(user);
    }

    public void unfollow(User user) {
        following.remove(user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(String profileImage) {
		this.profileImage = profileImage;
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getLinkedinUrl() {
		return linkedinUrl;
	}

	public void setLinkedinUrl(String linkedinUrl) {
		this.linkedinUrl = linkedinUrl;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Integer getReputation() {
		return reputation;
	}

	public void setReputation(Integer reputation) {
		this.reputation = reputation;
	}

	public Integer getViews() {
		return views;
	}

	public void setViews(Integer views) {
		this.views = views;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	public Set<Question> getVotedQuestions() {
		return votedQuestions;
	}

	public void setVotedQuestions(Set<Question> votedQuestions) {
		this.votedQuestions = votedQuestions;
	}

	public Set<Answer> getVotedAnswers() {
		return votedAnswers;
	}

	public void setVotedAnswers(Set<Answer> votedAnswers) {
		this.votedAnswers = votedAnswers;
	}

	public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Boolean getIsBanned() {
		return isBanned;
	}

	public void setIsBanned(Boolean isBanned) {
		this.isBanned = isBanned;
	}

	public LocalDateTime getBannedUntil() {
		return bannedUntil;
	}

	public void setBannedUntil(LocalDateTime bannedUntil) {
		this.bannedUntil = bannedUntil;
	}

	public String getBanReason() {
		return banReason;
	}

	public void setBanReason(String banReason) {
		this.banReason = banReason;
	}

	public Boolean getEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(Boolean emailVerified) {
		this.emailVerified = emailVerified;
	}

	public Boolean getTwoFactorEnabled() {
		return twoFactorEnabled;
	}

	public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
		this.twoFactorEnabled = twoFactorEnabled;
	}

	public String getTwoFactorSecret() {
		return twoFactorSecret;
	}

	public void setTwoFactorSecret(String twoFactorSecret) {
		this.twoFactorSecret = twoFactorSecret;
	}

	public Set<User> getFollowing() {
		return following;
	}

	public void setFollowing(Set<User> following) {
		this.following = following;
	}

	public Set<User> getFollowers() {
		return followers;
	}

	public void setFollowers(Set<User> followers) {
		this.followers = followers;
	}
}

