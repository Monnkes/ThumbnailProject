package agh.project.oot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Setter
@Getter
@Table("folders")
@EqualsAndHashCode
public class Folder implements Comparable<Folder> {

    @Id
    private Long id;
    private Long parentId = 0L;
    private String name;

    public Folder() {}

    public Folder(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    @Override
    public int compareTo(Folder folder) {
        return this.id.compareTo(folder.id);
    }
}
