package org.example.service;
import org.example.dao.CommentDAO;
import org.example.model.Comment;
import org.example.model.Review;
import org.example.model.User;

public class CommentService {
    private final CommentDAO commentDao;

    public CommentService(CommentDAO commentDao) {
        this.commentDao = commentDao;
    }

    public Comment addComent(User user, Review review, String content) throws InvalidDataException{
        if(content==null||content.trim().isEmpty()){
            throw new InvalidDataException("Datele introduse nu sunt valide!");
        }

        Comment newComment = new Comment(user, review, content);
        commentDao.save(newComment);

        return newComment;

    }

    public void deleteComment(int commentId) throws InvalidDataException {
        Comment comment = commentDao.findById(commentId);
        if (comment == null) {
            throw new InvalidDataException("Comentariul căutat nu există!");
        }
        commentDao.delete(comment);
    }

    public void reportComment(int commentId) {
        Comment comment = commentDao.findById(commentId);
        if (comment != null) {
            comment.setReportCount(comment.getReportCount() + 1);
            if (comment.getReportCount() >= 5) {
                commentDao.delete(comment);
            } else {
                commentDao.update(comment);
            }
        }
    }
}
