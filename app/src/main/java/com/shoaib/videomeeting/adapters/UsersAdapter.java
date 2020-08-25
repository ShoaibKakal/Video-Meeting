package com.shoaib.videomeeting.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.shoaib.videomeeting.R;
import com.shoaib.videomeeting.listeners.UsersListener;
import com.shoaib.videomeeting.models.User;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    private List<User> users;
    private UsersListener usersListener;
    private List<User> selectedUsers;

    public UsersAdapter(List<User> users, UsersListener usersListener) {
        this.users = users;
        this.usersListener = usersListener;
        selectedUsers = new ArrayList<>();
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setUserData(users.get(position));

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textFirstChar, textUsername, textEmail;
        private ImageView imageVideoMeeting, imageAudioMeeting;
        private ConstraintLayout userContainer;
        private ImageView imageSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFirstChar = itemView.findViewById(R.id.textFirstChar);
            textEmail = itemView.findViewById(R.id.textEmail);
            textUsername = itemView.findViewById(R.id.textUsername);
            imageAudioMeeting = itemView.findViewById(R.id.imageAudioMeeting);
            imageVideoMeeting = itemView.findViewById(R.id.imageVideoMeeting);
            userContainer = itemView.findViewById(R.id.userContainer);
            imageSelected = itemView.findViewById(R.id.imageSelected);

        }

        public void setUserData(User user){
            textFirstChar.setText(user.firstName.substring(0,1));
            textEmail.setText(user.email);
            textUsername.setText(String.format("%s %s",user.firstName, user.lastName));

            imageVideoMeeting.setOnClickListener(v -> usersListener.initiateVideoMeeting(user));

            imageAudioMeeting.setOnClickListener(v -> usersListener.initiateAudioMeeting(user));

            userContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (imageSelected.getVisibility() != View.VISIBLE){
                        selectedUsers.add(user);
                        imageSelected.setVisibility(View.VISIBLE);
                        imageAudioMeeting.setVisibility(View.GONE);
                        imageVideoMeeting.setVisibility(View.GONE);
                        usersListener.onMultipleUsersAction(true);
                    }

                    return true;
                }
            });

            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (imageSelected.getVisibility() == View.VISIBLE){
                        selectedUsers.remove(user);
                        imageSelected.setVisibility(View.GONE);
                        imageVideoMeeting.setVisibility(View.VISIBLE);
                        imageAudioMeeting.setVisibility(View.VISIBLE);

                        if (selectedUsers.size()==0){
                            usersListener.onMultipleUsersAction(false);
                        }

                    }
                    else{
                        if (selectedUsers.size()>0) {
                            selectedUsers.add(user);
                            imageSelected.setVisibility(View.VISIBLE);
                            imageAudioMeeting.setVisibility(View.GONE);
                            imageVideoMeeting.setVisibility(View.GONE);
                        }
                    }
                }
            });



        }
    }
}
