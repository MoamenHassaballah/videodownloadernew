

package avd.downloader.history_feature;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import avd.downloader.LMvdActivity;
import avd.downloader.LMvdFragment;
import avd.downloader.R;
import avd.downloader.utils.Utils;

public class History extends LMvdFragment implements LMvdActivity.OnBackPressedListener {
    private View view;
    private EditText searchText;
    private RecyclerView visitedPagesView;

    private List<VisitedPage> visitedPages;
    private HistorySQLite historySQLite;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        if (view == null) {
            getLMvdActivity().setOnBackPressedListener(this);

            view = inflater.inflate(R.layout.history, container, false);
            searchText = view.findViewById(R.id.historySearchText);
            ImageView searchButton = view.findViewById(R.id.historySearchIcon);
            visitedPagesView = view.findViewById(R.id.visitedPages);
            TextView clearHistory = view.findViewById(R.id.clearHistory);

            historySQLite = new HistorySQLite(getActivity());
            visitedPages = historySQLite.getAllVisitedPages();

            visitedPagesView.setAdapter(new VisitedPagesAdapter());
            visitedPagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
            visitedPagesView.addItemDecoration(Utils.createDivider(getActivity()));

            clearHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    historySQLite.clearHistory();
                    visitedPages.clear();
                    visitedPagesView.getAdapter().notifyDataSetChanged();
                }
            });

            searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    searchGo();
                    return false;
                }
            });

            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchGo();
                }
            });
        }

        return view;
    }

    private void searchGo() {
        if (getActivity().getCurrentFocus() != null) {
            Utils.hideSoftKeyboard(getActivity(), getActivity().getCurrentFocus().getWindowToken());
            visitedPages = historySQLite.getVisitedPagesByKeyword(searchText.getText()
                    .toString());
            visitedPagesView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onBackpressed() {
        getLMvdActivity().getBrowserManager().unhideCurrentWindow();
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    private class VisitedPagesAdapter extends RecyclerView.Adapter<VisitedPagesAdapter.VisitedPageItem> {
        @NonNull
        @Override
        public VisitedPageItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            return new VisitedPageItem(inflater.inflate(R.layout.history_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VisitedPageItem holder, int position) {
            holder.bind(visitedPages.get(position));
        }

        @Override
        public int getItemCount() {
            return visitedPages.size();
        }

        class VisitedPageItem extends RecyclerView.ViewHolder {
            private TextView title;

            VisitedPageItem(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.visitedPageTitle);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getLMvdActivity().browserClicked();
                        getLMvdActivity().getBrowserManager().newWindow(visitedPages.get
                                (getAdapterPosition()).link);
                    }
                });
                itemView.findViewById(R.id.visitedPageDelete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        historySQLite.deleteFromHistory(visitedPages.get(getAdapterPosition()).link);
                        visitedPages.remove(getAdapterPosition());
                        notifyItemRemoved(getAdapterPosition());
                    }
                });
            }

            void bind(VisitedPage page) {
                title.setText(page.title);
            }
        }
    }
}
